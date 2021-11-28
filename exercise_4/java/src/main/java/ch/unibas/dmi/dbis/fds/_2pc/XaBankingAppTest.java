package ch.unibas.dmi.dbis.fds._2pc;

import org.junit.jupiter.api.*;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Check the XA stuff here --> https://docs.oracle.com/cd/B14117_01/java.101/b10979/xadistra.htm
 *
 * @author Alexander Stiemer (alexander.stiemer at unibas.ch)
 */
public class XaBankingAppTest {

    private static final Logger LOG = Logger.getLogger( XaBankingAppTest.class.getName() );

    /** TODO 1 of 2: Change login credentials to the ones you received via mail. */
    private static final String DBMS_USERNAME = "fdis_1";
    private static final String DBMS_PASSWORD = "AUUjFGQ";


    static {
        if ( DBMS_USERNAME.equalsIgnoreCase( "Your username" ) ) {
            throw new IllegalArgumentException( "Please set your username properly." );
        }
        if ( DBMS_PASSWORD.equalsIgnoreCase( "Your password" ) ) {
            throw new IllegalArgumentException( "Please set your password properly." );
        }
    }


    public static void main( String[] args ) {
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(XaBankingAppTest.class)).build();
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        System.out.println("getTestsSucceededCount() - " + summary.getTestsSucceededCount());
        failures.forEach(failure -> System.out.println("failure - " + failure.getException()));

        failures.forEach(f -> f.getException().printStackTrace());
    }


    private enum Bank {
        /** TODO 2 of 2: Adapt connection details according to exercise sheet. */
        BANK_X( "p5", "jdbc:oracle:thin:@p5.dmi.unibas.ch:1521:xe", DBMS_USERNAME, DBMS_PASSWORD ),
        BANK_Y( "p6", "jdbc:oracle:thin:@p6.dmi.unibas.ch:1521:xe", DBMS_USERNAME, DBMS_PASSWORD );

        public final transient AbstractOracleXaBank bank;


        Bank( final String BIC, final String jdbcConnectionString, final String dbmsUsername, final String dbmsPassword ) {
            try {
                this.bank = new OracleXaBank( BIC, jdbcConnectionString, dbmsUsername, dbmsPassword );
            } catch ( SQLException ex ) {
                throw new InternalError( "Exception while creating the bank object.", ex );
            }
        }
    }


    @BeforeAll
    public static void initClass() throws Exception {
    }


    @AfterAll
    public static void cleanupClass() {
        for ( Bank b : Bank.values() ) {
            b.bank.closeConnection();
        }
    }


    @BeforeEach
    public void beforeTest() throws SQLException {
        resetAccountBalances();
        System.out.printf( "\n-----------------------------------------\n" );
    }


    private void resetAccountBalances() throws SQLException {
        for ( Bank b : Bank.values() ) {
            try ( Connection c = DriverManager.getConnection( b.bank.jdbcConnectionString, DBMS_USERNAME, DBMS_PASSWORD ) ) {
                c.setAutoCommit( false );

                Statement statement = c.createStatement();
                statement.executeUpdate( "UPDATE account SET balance = 8000 WHERE iban = 'CH5367B1'" );
                statement.executeUpdate( "UPDATE account SET balance = 15000 WHERE iban = 'CH5367B2'" );
                statement.executeUpdate( "UPDATE account SET balance = 5000 WHERE iban = 'CH5367B3'" );
                statement.executeUpdate( "UPDATE account SET balance = 1700 WHERE iban = 'CH5367B4'" );
                statement.executeUpdate( "UPDATE account SET balance = 2345 WHERE iban = 'CH5367B5'" );
                c.commit();
            }
        }
    }


    @AfterEach
    public void afterTest() {
        System.out.printf( "-----------------------------------------\n" );
    }


    private void printTestDescription( final String testName, final String ibanFrom, final String bicFrom, final String ibanTo, final String bicTo, final float transferValue ) {
        System.out.printf( "%s\n", testName );
        System.out.printf( "Move %1.2f from %s on %s to %s on %s.\n", transferValue, ibanFrom, bicFrom, ibanTo, bicTo );
    }


    private void printBalance( final boolean before, final String iban, final String bic, final float balance ) {
        System.out.printf( ("Account balance for %s at bank %s " + (before ? "BEFORE" : "AFTER") + " the transaction: %1.2f\n"), iban, bic, balance );
    }


    @Test
    public void transfer() throws SQLException {
        final String ibanFrom = "CH5367B1", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        final String ibanTo = "CH5367B1", bicTo = Bank.BANK_Y.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_Y.bank;

        final float transferValue = 100.5f;

        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom ) - transferValue;
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo ) + transferValue;

        assertFalse( Float.isNaN( expectedBalanceFrom ) );
        assertFalse( Float.isNaN( expectedBalanceTo ) );

        //
        printTestDescription( "Transfer", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

    @Test
    public void transferNegative() throws SQLException {
        final String ibanFrom = "CH5367B1", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        final String ibanTo = "CH5367B1", bicTo = Bank.BANK_Y.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_Y.bank;

        final float transferValue = -100.5f;

        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom );
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo );

        assertFalse( Float.isNaN( expectedBalanceFrom ) );
        assertFalse( Float.isNaN( expectedBalanceTo ) );

        //
        printTestDescription( "Transfer negative money", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

    @Test
    public void transferWrongIban() throws SQLException {
        final String ibanFrom = "CH5367B1", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        // note: this iban does not exist!
        final String ibanTo = "CH5367B6", bicTo = Bank.BANK_Y.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_Y.bank;

        final float transferValue = 100.5f;

        // Expect nothing to change
        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom );
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo );

        assertFalse( Float.isNaN( expectedBalanceFrom ) );
        assertTrue( Float.isNaN( expectedBalanceTo ) );

        printTestDescription( "Transfer to wrong IBAN", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

    @Test
    public void transferFromWrongIban() throws SQLException {
        // note: this iban does not exist!
        final String ibanFrom = "CH5367B6", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        final String ibanTo = "CH5367B1", bicTo = Bank.BANK_Y.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_Y.bank;

        final float transferValue = 100.5f;

        // Expect nothing to change
        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom );
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo );

        assertTrue( Float.isNaN( expectedBalanceFrom ) );
        assertFalse( Float.isNaN( expectedBalanceTo ) );

        printTestDescription( "Transfer from wrong IBAN", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

    @Test
    public void transferNotEnoughMoney() throws SQLException {
        final String ibanFrom = "CH5367B4", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        final String ibanTo = "CH5367B1", bicTo = Bank.BANK_Y.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_Y.bank;

        // note: more money than available in bank account.
        final float transferValue = 1800f;

        // Expect nothing to change
        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom );
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo );

        assertFalse( Float.isNaN( expectedBalanceFrom ) );
        assertFalse( Float.isNaN( expectedBalanceTo ) );

        printTestDescription( "Transfer with not enough money", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

    /*
     * This doesn't work because we share the same connection for both databases and therefore the
     * transactions get mixed up (?).
     */
    @Test
    public void transferSameBank() throws SQLException {
        final String ibanFrom = "CH5367B4", bicFrom = Bank.BANK_X.name();
        final AbstractOracleXaBank FROM_BANK = Bank.valueOf( bicFrom ).bank;

        final String ibanTo = "CH5367B5", bicTo = Bank.BANK_X.name();
        final AbstractOracleXaBank TO_BANK = Bank.BANK_X.bank;

        // note: more money than available in bank account.
        final float transferValue = 1800f;

        // Expect nothing to change
        final float expectedBalanceFrom = FROM_BANK.getBalance( ibanFrom );
        final float expectedBalanceTo = TO_BANK.getBalance( ibanTo );

        assertFalse( Float.isNaN( expectedBalanceFrom ) );
        assertFalse( Float.isNaN( expectedBalanceTo ) );

        printTestDescription( "Transfer to same bank", ibanFrom, bicFrom, ibanTo, bicTo, transferValue );
        printBalance( true, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
        printBalance( true, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

        try {
            System.out.println( "-- executing transfer --" );
            FROM_BANK.transfer( TO_BANK, ibanFrom, ibanTo, transferValue );
        } finally {
            printBalance( false, ibanFrom, bicFrom, FROM_BANK.getBalance( ibanFrom ) );
            printBalance( false, ibanTo, bicTo, TO_BANK.getBalance( ibanTo ) );

            assertEquals( expectedBalanceFrom, FROM_BANK.getBalance( ibanFrom ), Float.MIN_VALUE );
            assertEquals( expectedBalanceTo, TO_BANK.getBalance( ibanTo ), Float.MIN_VALUE );
        }
    }

}
