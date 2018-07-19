package org.ebayopensource.fidouaf.res.util;

import org.ebayopensource.fido.uaf.storage.AuthenticatorRecord;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {

    private static final DBConnection instance = new DBConnection();
    String url;
    String user;
    String host;
    String port;
    String db_name;
    String password;
    Logger lgr = Logger.getLogger(DBConnection.class.getName());
    Connection con;

    //private constructor to avoid client applications to use constructor
    private DBConnection(){
        host = getSysEnv("DB_HOST", "localhost");
        port = getSysEnv("DB_PORT", "5432");
        user = getSysEnv("DB_USER", "postgres");
        db_name = getSysEnv("DB_NAME", "fido");
        password = getSysEnv("DB_PASSWORD", "");
        url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db_name);

        try {
            DriverManager.registerDriver(new org.postgresql.Driver());
            con = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String getSysEnv(String key, String default_value){
        String val = System.getenv(key);

        return (val != null && !val.isEmpty()) ? val : default_value;
    }

    public static DBConnection getInstance() {
        return instance;
    }

    public String version() {

        try (
             Statement st = this.con.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {

            if (rs.next()) {
                return rs.getString(1);
            }

        } catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "Null";
    }

    public String saveRegistrationRecord(RegistrationRecord rr) {
        try (
             Statement st = this.con.createStatement())
        {
            String authenticator_id = saveAuthenticatorRecord(rr.authenticator);
            rr.authenticator_id = authenticator_id;
            ResultSet rs = st.executeQuery(this.prepareInsertRegistrationRecord(rr));
            if (rs.next()) {
                return rs.getString(1);
            }

        }catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        return null;
    }

    public RegistrationRecord getRecordByKeyAndAAID(String key_id, String aaid) {
        try (
                Statement st = this.con.createStatement())
        {
            ResultSet rs = st.executeQuery(this.prepareGetRecordByKeyAndAAID(key_id, aaid));
            while (rs.next()) {
                RegistrationRecord regRecord = new RegistrationRecord();
                regRecord.authenticator_id = rs.getString("authenticator_id");
                regRecord.PublicKey = rs.getString("public_key");
                regRecord.SignCounter = rs.getString("sign_counter");
                regRecord.AuthenticatorVersion = rs.getString("authenticator_version");
                regRecord.tcDisplayPNGCharacteristics = rs.getString("tc_display_png_characteristics");
                regRecord.username = rs.getString("username");
                regRecord.userId = rs.getString("user_id");
                regRecord.deviceId = rs.getString("device_id");
                regRecord.timeStamp = rs.getString("time_stamp");
                regRecord.status = rs.getString("status");
                regRecord.attestCert = rs.getString("attest_cert");
                regRecord.attestDataToSign = rs.getString("attest_data_to_sign");
                regRecord.attestSignature = rs.getString("attest_signature");
                regRecord.attestVerifiedStatus = rs.getString("attest_verified_status");

                return regRecord;
            }

        }catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        return null;
    }

    public String saveAuthenticatorRecord(AuthenticatorRecord ar) {
        try (
                Statement st = this.con.createStatement())
        {
            ResultSet rs = st.executeQuery(this.prepareAuthenticatorRecord(ar));
            if (rs.next()) {
                return rs.getString(1);
            }

            }catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        return null;
    }

    public Integer authenticationRecordCount(AuthenticatorRecord ar) {
        try (
                Statement st = this.con.createStatement())
        {
            ResultSet rs = st.executeQuery(this.prepareAuthenticatorRecord(ar));
            if (rs.next()) {
                return rs.getInt(1);
            }

        }catch (SQLException ex) {
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            return 0;
        }
        return 0;
    }


    private String prepareAuthenticatorRecordCount(AuthenticatorRecord ar) {
        return String.format(
                        "SELECT count(1) FROM authenticators \n" +
                        " WHERE aaid=%s AND key_id=%s AND device_id=%s AND username=%s AND status=%s);",
                ar.AAID, ar.KeyID, ar.deviceId, ar.username, ar.status);
    }

    private String prepareAuthenticatorRecord(AuthenticatorRecord ar) {
        return String.format(
                    "INSERT INTO public.authenticators(aaid, key_id, device_id, username, status) " +
                    "VALUES ('%s', '%s', '%s', '%s', '%s') " +
                    "RETURNING ID;",
                ar.AAID, ar.KeyID, ar.deviceId, ar.username, ar.status);
    }

    private String prepareInsertRegistrationRecord(RegistrationRecord rr) {
        return String.format(
                "INSERT INTO public.registrations(\n" +
                        "            authenticator_id, public_key, sign_counter, authenticator_version, \n" +
                        "            tc_display_png_characteristics, username, user_id, device_id, \n" +
                        "            time_stamp, status, attest_cert, attest_data_to_sign, attest_signature, \n" +
                        "            attest_verified_status)\n" +
                        "    VALUES ('%s', '%s', '%s', '%s', '%s', \n" +
                        "            '%s', '%s', '%s', '%s', '%s', \n" +
                        "            '%s', '%s', '%s', '%s')\n" +
                        "    RETURNING ID;",
                rr.authenticator_id, rr.PublicKey, rr.SignCounter, rr.AuthenticatorVersion, 
                rr.tcDisplayPNGCharacteristics, rr.username, rr.userId, rr.deviceId, rr.timeStamp, 
                rr.status, rr.attestCert, rr.attestDataToSign, rr.attestSignature, rr.attestVerifiedStatus);
    }

    private String prepareGetRecordByKeyAndAAID(String key, String aaid) {
        return String.format(
                "SELECT r.* " +
                "FROM registrations AS r " +
                "JOIN authenticators AS a ON (r.authenticator_id = a.id) " +
                "WHERE a.key_id = '%s' and a.aaid = '%s'" +
                "LIMIT 1;",
                key, aaid
        );
    }

}