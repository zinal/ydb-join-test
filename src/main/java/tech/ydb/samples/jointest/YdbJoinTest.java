package tech.ydb.samples.jointest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 *
 * @author mzinal
 */
public class YdbJoinTest implements AutoCloseable {

    private Connection connection = null;

    public static void main(String[] args) {
        try (YdbJoinTest instance = new YdbJoinTest()) {
            instance.init();
            instance.run();
        } catch(Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public void run() throws Exception {
        makeTables();
        insertRows();
        selectRows();
    }

    public void init() throws Exception {
        String connString = System.getenv("YDB_CONNECTION_STRING");
        if (connString==null) {
            connString = "grpc://localhost:2135/local";
        }
        String jdbcUrl = "jdbc:ydb:" + connString + "?useQueryService=true";
        String tlsFile = System.getenv("YDB_SSL_ROOT_CERTIFICATES_FILE");
        if (tlsFile!=null) {
            jdbcUrl = jdbcUrl + "&secureConnectionCertificate=file:" + tlsFile;
        }
        String login = System.getenv("YDB_USER");
        if (login==null) {
            String saFile = System.getenv("YDB_SERVICE_ACCOUNT_KEY_FILE_CREDENTIALS");
            if (saFile!=null) {
                jdbcUrl = jdbcUrl + "&saFile=file:" + saFile;
            }
            this.connection = DriverManager.getConnection(jdbcUrl);
        } else {
            this.connection = DriverManager.getConnection(jdbcUrl, login, System.getenv("YDB_PASSWORD"));
        }
        this.connection.setAutoCommit(false);
    }

    @Override
    public void close() {
        if (connection!=null) {
            try {
                connection.close();
            } catch(Exception ex) {}
            connection = null;
        }
    }

    private void makeTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("create table ja(a text, b text, c text, primary key(a))");
            stmt.execute("create table jb(b text, bv text, primary key(b))");
            stmt.execute("create table jc(c text, cv text, primary key(c))");
        }
        System.out.println("created the ja, jb, jc tables!");
    }

    private void insertRows() throws Exception {
        System.out.println("writing...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("upsert into ja(a,b,c) values('1'u, '1001'u, '2001'u),('2'u, '1002'u, '2002'u), ('3'u, '1003'u, '2003'u)");
            stmt.execute("upsert into jb(b, bv) values('1001'u,'b 1001'u),('1002'u,'b 1002'u),('1003'u,'b 1003'u)");
            stmt.execute("upsert into jc(c, cv) values('7001'u,'c 7001'u),('7002'u,'c 7002'u),('7003'u,'c 7003'u)");
        }
        connection.commit();
        System.out.println("committed!");
    }

    private void selectRows() throws Exception {
        System.out.println("reading...");
        String sql = "select ja.a, jb.bv, jc.cv from ja inner join jb on ja.b=jb.b left join jc on ja.c=jc.cv;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("... "
                            + rs.getString(1) + ", "
                            + rs.getString(2) + ", "
                            + rs.getString(3));
                }
            }
        }
        System.out.println("ready!");
    }
}
