package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.library.entity.LibraryAccountStatus;
import edu.seu.vcampus.common.user.entity.Role;
import java.sql.SQLException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证读者账户建档、撤销和借阅资格。 */
class LibraryAccountServiceTest {
    private LibraryAccountDao dao;
    private LibraryAccountProvisioner provisioner;

    @BeforeEach
    void setUp() {
        dao = mock(LibraryAccountDao.class);
        provisioner = new LibraryAccountProvisioner(dao);
    }

    @Test
    void provisionsStudentAndTeacherAccounts() throws Exception {
        when(dao.insert(any(LibraryAccount.class))).thenReturn(true);

        provisioner.provision("student-id", "学生", Role.STUDENT);
        provisioner.provision("teacher-id", "教师", Role.TEACHER);

        ArgumentCaptor<LibraryAccount> captured = ArgumentCaptor.forClass(LibraryAccount.class);
        verify(dao, org.mockito.Mockito.times(2)).insert(captured.capture());
        assertEquals(30, captured.getAllValues().get(0).getBorrowLimit());
        assertEquals(LibraryAccountStatus.NORMAL,
                captured.getAllValues().get(1).getStatus());
    }

    @Test
    void skipsAdminAndDoesNotDuplicateExistingAccount() throws Exception {
        LibraryAccount existing = account("student-id");
        when(dao.findByUserUuid("student-id")).thenReturn(existing);

        provisioner.provision("admin-id", "管理员", Role.ADMIN);
        provisioner.provision("student-id", "学生", Role.STUDENT);

        verify(dao, never()).insert(any(LibraryAccount.class));
    }

    @Test
    void revokeSoftDeletesExistingAccount() throws Exception {
        when(dao.findByUserUuid("student-id")).thenReturn(account("student-id"));
        when(dao.softDelete(org.mockito.ArgumentMatchers.eq("student-id"),
                any(Date.class))).thenReturn(true);

        provisioner.revoke("student-id");

        verify(dao).softDelete(org.mockito.ArgumentMatchers.eq("student-id"),
                any(Date.class));
    }

    @Test
    void accountServiceReturnsAccountAndEnforcesStateAndLimit() throws Exception {
        final LibraryAccount account = account("student-id");
        when(dao.findByUserUuid("student-id")).thenReturn(account);
        final LibraryAccountService service = new LibraryAccountService(dao);

        assertSame(account, service.query("student-id"));
        LibraryException full = assertThrows(LibraryException.class, new Executable() {
            @Override
            public void execute() throws Exception {
                service.ensureCanBorrow("student-id", 30);
            }
        });
        assertEquals(StatusCode.BAD_REQUEST, full.getStatusCode());

        account.setStatus(LibraryAccountStatus.SUSPENDED);
        LibraryException suspended = assertThrows(LibraryException.class, new Executable() {
            @Override
            public void execute() throws Exception {
                service.ensureCanBorrow("student-id", 0);
            }
        });
        assertEquals(StatusCode.FORBIDDEN, suspended.getStatusCode());
    }

    @Test
    void translatesStorageFailureToProvisioningFailure() throws Exception {
        when(dao.findByUserUuid("student-id")).thenThrow(new SQLException("offline"));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        provisioner.provision("student-id", "学生", Role.STUDENT);
                    }
                });
        assertFalse(failure.getMessage().isEmpty());
        assertSame(SQLException.class, failure.getCause().getClass());
    }

    private LibraryAccount account(String userUuid) {
        return new LibraryAccount(userUuid, 30, new Date());
    }
}
