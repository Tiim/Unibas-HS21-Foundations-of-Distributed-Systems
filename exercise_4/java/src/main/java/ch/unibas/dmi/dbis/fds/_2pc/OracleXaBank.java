package ch.unibas.dmi.dbis.fds._2pc;


import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Check the XA stuff here --> https://docs.oracle.com/cd/B14117_01/java.101/b10979/xadistra.htm
 *
 * @author Alexander Stiemer (alexander.stiemer at unibas.ch)
 */
public class OracleXaBank extends AbstractOracleXaBank {


    public OracleXaBank( final String BIC, final String jdbcConnectionString, final String dbmsUsername, final String dbmsPassword ) throws SQLException {
        super( BIC, jdbcConnectionString, dbmsUsername, dbmsPassword );
    }


    @Override
    public float getBalance( final String iban ) throws SQLException {
        float balance = Float.NaN;
        Connection c = getXaConnection().getConnection();

        PreparedStatement stmt = c.prepareStatement("SELECT Balance FROM account WHERE IBAN = ?");
        stmt.setString(1, iban);
        ResultSet result = stmt.executeQuery();
        if (result.next()) {
             return result.getFloat(1);
        }
        return balance;
    }


    @Override
    public void transfer( final AbstractOracleXaBank TO_BANK, final String ibanFrom, final String ibanTo, final float value ) {

        System.out.println("Starting transfer");
        Xid curXid = null;
        Xid toXid = null;

        if (value <= 0) {
            System.out.println("Can't transfer negative money!");
            return;
        }

        try {
            curXid = startTransaction();
            toXid = TO_BANK.startTransaction();

            Connection curCon = getXaConnection().getConnection();
            Connection toCon = TO_BANK.getXaConnection().getConnection();

            // not needed because of database constraint.
            // somehow causes the connection to close?
            /*
            if (getBalance(ibanFrom) < value) {
                throw new Exception("Can't transfer more than is available in account #" + ibanFrom);
            }
            */

            PreparedStatement curStmt = curCon.prepareStatement("UPDATE account SET Balance = Balance - ? WHERE IBAN = ?");
            curStmt.setFloat(1, value);
            curStmt.setString(2, ibanFrom);

            PreparedStatement toStmt = toCon.prepareStatement("UPDATE account SET Balance = Balance + ? WHERE IBAN = ?");
            toStmt.setFloat(1, value);
            toStmt.setString(2, ibanTo);

            int curRows = curStmt.executeUpdate();
            int toRows = toStmt.executeUpdate();

            if (curRows == 0 || toRows == 0) {
                // no rows were updated: iban is wrong!
                throw new SQLException("No updated rows!");
            }

            endTransaction(curXid, false);
            TO_BANK.endTransaction(toXid, false);
            System.out.println("Transfer succeeded");
        } catch (Exception e) {
            System.out.println("Transfer failed!");
            System.out.println("Rolling back due to: " + e.getMessage());
            //e.printStackTrace(System.out);
            failBothTransactions(curXid, toXid, TO_BANK);
        }
    }


    private void failBothTransactions(Xid curXid, Xid toXid, AbstractOracleXaBank TO_BANK) {
        if (curXid != null) {
            try {
                endTransaction(curXid, true);
            } catch (XAException err) {
                err.printStackTrace();
            }
        }

        if (toXid != null) {
            try {
                TO_BANK.endTransaction(toXid, true);
            } catch (XAException err) {
                err.printStackTrace();
            }
        }
    }
}
