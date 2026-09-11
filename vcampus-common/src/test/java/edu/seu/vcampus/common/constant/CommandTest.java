package edu.seu.vcampus.common.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Command 中银行命令码号段和唯一性测试。 */
class CommandTest {

    @Test
    void bankCommandsUseUniqueSixHundredRangeValues() {
        Set<Integer> commands = new HashSet<Integer>();
        commands.add(Command.BANK_ACCOUNT_QUERY);
        commands.add(Command.BANK_RECHARGE);
        commands.add(Command.BANK_TRANSACTION_LIST);
        commands.add(Command.BANK_ACCOUNT_OPEN);

        assertEquals(4, commands.size());
        assertEquals(601, Command.BANK_ACCOUNT_QUERY);
        assertEquals(602, Command.BANK_RECHARGE);
        assertEquals(603, Command.BANK_TRANSACTION_LIST);
        assertEquals(604, Command.BANK_ACCOUNT_OPEN);
        for (Integer command : commands) {
            assertTrue(command >= 600 && command <= 699);
        }
    }

    @Test
    void allCommandCodesAreUnique() throws Exception {
        Set<Integer> commands = new HashSet<Integer>();
        for (Field command : Command.class.getFields()) {
            if (command.getType() == int.class) {
                assertTrue(commands.add(command.getInt(null)), command.getName());
            }
        }
    }
}
