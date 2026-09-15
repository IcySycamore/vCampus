package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.UserRepository;
import edu.seu.vcampus.server.user.UserRepository.Credential;
import java.util.Collections;
import java.util.List;

/** 把图书馆账户接入用户生命周期，并为既有师生账户补档。 */
final class LibraryAccountRegistration {
    private LibraryAccountRegistration() {
    }

    static void register(LibraryService service, AccountProvisioning provisioning) {
        if (service == null || provisioning == null) {
            return;
        }
        LibraryAccountProvisioner provisioner = service.getAccountProvisioner();
        provisioning.add(provisioner);
        int handled = provisionExisting(provisioner, existingAccounts());
        System.out.println("图书馆：核对了 " + handled + " 个既有账号的读者账户");
    }

    static int provisionExisting(LibraryAccountProvisioner provisioner,
            List<Credential> users) {
        if (provisioner == null || users == null) {
            return 0;
        }
        int handled = 0;
        for (Credential user : users) {
            if (user == null || user.getUuid() == null) {
                continue;
            }
            String role = user.getRole();
            provisioner.provision(user.getUuid(), user.getDisplayName(),
                    role == null ? null : Role.fromDisplayName(role));
            handled++;
        }
        return handled;
    }

    private static List<Credential> existingAccounts() {
        UserRepository users = AuthModule.repository();
        return users == null ? Collections.<Credential>emptyList() : users.findAll();
    }
}
