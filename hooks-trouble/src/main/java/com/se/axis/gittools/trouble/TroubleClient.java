/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter; // Base64 support

/**
 * Trouble client.
 *
 * Used for interfacing with Trouble.
 */
public final class TroubleClient {

  private static final int TIMEOUT_CONNECT_MILLIS = 5000;
  private static final int TIMEOUT_READ_MILLIS = 30000;

  private static final String FORMAT_TICKET_GET = "%s/tickets/%d.json";
  private static final String FORMAT_COMMENT_POST = "%s/tickets/%d/comments.json";
  private static final String FORMAT_GET_POST_PACKAGE = "%s/tickets/%d/packages.json";
  private static final String FORMAT_PUT_DELETE_PACKAGE = "%s/tickets/%d/packages/%d.json";
  private static final String FORMAT_GET_POST_CORRECTION = "%s/tickets/%d/corrections.json";
  private static final String FORMAT_PUT_DELETE_CORRECTION = "%s/tickets/%d/corrections/%d.json";
  private static final String FORMAT_DELETE_PACKAGE_FROM_CORRECTION = "%s/tickets/%d/corrections/%d/packages/%d.json";

  private static final Logger LOG = LoggerFactory.getLogger(TroubleClient.class);
  private static final GsonBuilder GSON = new GsonBuilder();

  private final String baseApiUrl;
  private final String apiUser;
  private final String apiPass;

  private final String impersonatedUser;
  private final int ticketId;

  /**
   * Exception thrown when trouble responds with a non-successfult result code.
   */
  public static class HttpException extends IOException {
    /**
     * The HTTP response code.
     */
    public final int responseCode;

    /**
     * The HTTP response message.
     */
    public final String responseMessage;

    /**
     * The HTTP response body.
     */
    public final String responseBody;


    /**
     * Constructor.
     */
    public HttpException(final int code, final String message, final String body) {
      super(String.valueOf(code) + " " + message);
      this.responseCode = code;
      this.responseMessage = message;
      this.responseBody = body;
    }
  }

  /**
   * Commit (SHA1) container.
   */
  public static class Reference {
    /**
     * Commit (SHA1).
     */
    @SerializedName("revision") private String reference;

    /**
     * Default constructor for serialization.
     */
    public Reference() {
    }

    /**
     * Helper constructor.
     */
    public Reference(final String reference) {
      this.reference = reference;
    }

    @Override
    public final int hashCode() {
      return reference != null ? reference.hashCode() : super.hashCode();
    }

    @Override
    public final String toString() {
      return String.valueOf(reference);
    }
  }

  /**
   * Package info.
   */
  public static class Package {
    /**
     * Trouble's package identifier.
     */
    public Integer id;

    /**
     * Trouble's package identifier (only used for correction).
     */
    @SerializedName("package_id") private Integer packageId;

    /**
     * Package name (path).
     */
    @SerializedName("package") public String name;

    /**
     * Target branch for the fix/feature.
     */
    @SerializedName("target_branch") public String targetBranch;

    /**
     * Fix branch name.
     */
    @SerializedName("fix_branch") public String fixBranch;

    /**
     * Fix branch reference (activation tag or commit).
     */
    @SerializedName("fix_tag") public Reference fixRef;

    /**
     * Gerrit PatchSet reference or pre-branch commit.
     */
    @SerializedName("pre_merge_tag") public Reference preMergeRef;

    /**
     * The release tag or slot tag.
     */
    @SerializedName("merge_tag") public Reference mergeRef;

    /**
     * CBM approval status.
     */
    @SerializedName("cbm_approved") public Boolean cbmApproved;

    /**
     * Daily build approval status.
     */
    @SerializedName("daily_build_ok") public Boolean dailyBuildOk;

    /**
     * The impersonated user name.
     */
    public String username;

    /**
     * The person who is in charge of the fix (typically same as username).
     *
     * Used in relevant POST/PUT requests.
     */
    @SerializedName("assigned_username") private String assignedUsername;

    /**
     * Default constructor for serialization.
     */
    public Package() {
    }

    /**
     * Helper constructor for createOrUpdateFix.
     */
    private Package(final Integer id) {
      this.packageId = id;
    }
  }

  /**
   * Package container.
   */
  private static class PackageContainer {
    /**
     * The contained package.
     */
    @SerializedName("package") private Package troublePackage;

    /**
     * Default constructor for serialization.
     */
    public PackageContainer() {
    }

    /**
     * Helper constructor.
     */
    private PackageContainer(final Package troublePackage) {
      this.troublePackage = troublePackage;
    }
  }

  /**
   * Comment info.
   */
  private static class Comment {
    /**
     * The comment free text.
     */
    private String text;

    /**
     * The impersonated user name.
     */
    private String username;

    /**
     * Default constructor for serialization.
     */
    public Comment() {
    }

    /**
     * Helper constructor.
     */
    private Comment(final String text, final String username) {
      this.text = text;
      this.username = username;
    }
  }

  /**
   * Comment container.
   */
  private static class CommentContainer {
    /**
     * A comment.
     */
    private Comment comment;

    /**
     * Default constructor for serialization.
     */
    public CommentContainer() {
    }

    /**
     * Helper constructor.
     */
    private CommentContainer(final Comment comment) {
      this.comment = comment;
    }
  }

  /**
   * The correction (fix).
   */
  private static class Correction {
    /**
     * The correction id.
     */
    private Integer id;

    /**
     * The correction title.
     */
    @SerializedName("fix_description") public String title;

    /**
     * The correction description.
     */
    @SerializedName("text") public String text;

    /**
     * The correction packages.
     */
    public Package[] packages;

    /**
     * The impersonated user name.
     */
    private String username;

    /**
     * Default constructor for serialization.
     */
    public Correction() {
    }

    /**
     * Helper constructor.
     */
    private Correction(final String targetBranch, final String username) {
      this.title = correctionTitle(targetBranch);
      this.text = title;
      this.username = username;
    }
  }

  /**
   * Correction container.
   */
  private static class CorrectionContainer {
    /**
     * A correction.
     */
    private Correction correction;

    /**
     * Default constructor for serialization.
     */
    public CorrectionContainer() {
    }

    /**
     * Helper constructor.
     */
    private CorrectionContainer(final Correction correction) {
      this.correction = correction;
    }
  }

  /**
   * Ticket info.
   */
  public static class Ticket {
    /**
     * Ticket subject line.
     */
    public String title;

    /**
     * Ticket description.
     */
    public String description;

    /**
     * Reproduction frequency code.
     */
    @SerializedName("frequency_id") public Integer frequency;

    /**
     * Severity code.
     */
    @SerializedName("severity_id") public Integer severity;
  }

  /**
   * Private constructor.
   */
  private TroubleClient(final String baseApiUrl, final String apiUser, final String apiPass,
      final int ticketId, final String impersonatedUser) {
    this.baseApiUrl = baseApiUrl;
    this.apiUser = apiUser;
    this.apiPass = apiPass;
    this.impersonatedUser = (impersonatedUser != null ? impersonatedUser : apiUser);
    this.ticketId = ticketId;
 }

  /**
   * Create a TroubleClient.
   */
  public static TroubleClient create(final String baseApiUrl, final String apiUser, final String apiPass,
      final int ticketId, final String impersonatedUser) {
    return new TroubleClient(baseApiUrl, apiUser, apiPass, ticketId, impersonatedUser);
  }

  /**
   * Create a TroubleClient.
   */
  public static TroubleClient create(final String baseApiUrl, final String apiUser, final String apiPass, final int ticketId) {
    return new TroubleClient(baseApiUrl, apiUser, apiPass, ticketId, null);
  }

  /**
   * Get ticket info from Trouble.
   */
  public TroubleClient.Ticket getTicket() throws IOException {
    String url = String.format(FORMAT_TICKET_GET, baseApiUrl, ticketId);
    TroubleClient.Ticket ticket = null;

    String content = getRequest(url, apiUser, apiPass);
    ticket = GSON.create().fromJson(content, TroubleClient.Ticket.class); // deserialize!
    return ticket;
  }

  /**
   * Get ticket info from Trouble.
   */
  public boolean ticketExists() throws IOException {
    boolean exists = false;
    try {
      getTicket();
      exists = true;
    } catch (TroubleClient.HttpException e) {
      if (e.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        throw e;
      }
    }
    return exists;
  }

  /**
   * Add a comment to trouble.
   */
  public void addComment(final String message) throws IOException {
    TroubleClient.Comment comment = new TroubleClient.Comment(message, impersonatedUser);
    String json = GSON.create().toJson(new CommentContainer(comment)); // serialize
    String url = String.format(FORMAT_COMMENT_POST, baseApiUrl, ticketId);
    sendJsonRequest(url, apiUser, apiPass, "POST", json);
  }

  /**
   * Get a package from Trouble.
   */
  public TroubleClient.Package[] getPackages(final String targetBranch) throws IOException {
    String url = String.format(FORMAT_GET_POST_PACKAGE, baseApiUrl, ticketId);
    if (targetBranch != null) {
      url += "?target_branch=" + urlEncode(targetBranch);
    }
    try {
      String content = getRequest(url.toString(), apiUser, apiPass);
      return GSON.create().fromJson(content, TroubleClient.Package[].class); // deserialize!
    } catch (TroubleClient.HttpException e) { // POST
      if (e.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        throw e;
      }
      return new TroubleClient.Package[0];
    }
  }

  /**
   * Create a package in Trouble.
   *
   * We cannot assume that the package is already added, since users can remove
   * it at any time. Hence we need to try update (PUT) first, and as a fallback,
   * we call add.
   */
  public TroubleClient.Package addOrUpdatePackage(final TroubleClient.Package troublePackage)
      throws IOException {
    // See if the package has already been added
    if (troublePackage.username == null) {
      troublePackage.username = impersonatedUser;
    }

    String json = null;
    if (troublePackage.id != null) { // update
      String url = String.format(FORMAT_PUT_DELETE_PACKAGE, baseApiUrl, ticketId, troublePackage.id);
      Integer id = troublePackage.id;
      troublePackage.id = null; // prevent serialization
      json = GSON.create().toJson(new PackageContainer(troublePackage)); // serialize
      troublePackage.id = id;
      json = sendJsonRequest(url, apiUser, apiPass, "PUT", json);
    } else { // create
      if (troublePackage.assignedUsername == null) {
        troublePackage.assignedUsername = impersonatedUser;
      }
      String url = String.format(FORMAT_GET_POST_PACKAGE, baseApiUrl, ticketId);
      json = GSON.create().toJson(new PackageContainer(troublePackage)); // serialize
      json = sendJsonRequest(url, apiUser, apiPass, "POST", json);
    }
    return GSON.create().fromJson(json, TroubleClient.Package.class); // deserialize!
  }

  /**
   * Delete a package in Trouble.
   */
  public void deletePackage(final int packageId) throws IOException {
    String url = String.format(FORMAT_PUT_DELETE_PACKAGE, baseApiUrl, ticketId, packageId)
      + "?username=" + urlEncode(impersonatedUser);
    try {
      deleteRequest(url, apiUser, apiPass);
    } catch (TroubleClient.HttpException e) {
      if (e.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        throw e;
      }
      LOG.debug("Package " + packageId + " already deleted in ticket " + ticketId);
    }
  }

  /**
   * Get a correction identifier (if any) from Trouble.
   */
  public TroubleClient.Correction getCorrection(final String targetBranch) throws IOException {
    StringBuilder url = new StringBuilder(String.format(FORMAT_GET_POST_CORRECTION, baseApiUrl, ticketId));
    String content = getRequest(url.toString(), apiUser, apiPass);

    String magic = correctionTitle(targetBranch);
    for (TroubleClient.Correction correction :  GSON.create().fromJson(content, TroubleClient.Correction[].class)) {
      if (magic.equals(correction.title)) {
        return correction;
      }
    }

    return null;
  }

  /**
   * Creates and/or updates the Gerrit correction info.
   *
   * Correction info is created/updated when the first package towards a specific target branch
   * is completely approved. Each time the correction info is updated all packages toward the SAME
   * target branch are added. This can also include the packages that are added by other users.
   *
   * When a package is deleted, it is implicitly removed from all correction infos.
   */
  public void createOrUpdateFix(final String targetBranch, final int packageId) throws IOException {

    // get the correction id
    TroubleClient.Correction correction = getCorrection(targetBranch);
    if (correction == null) { // create correction info
      correction = new TroubleClient.Correction(targetBranch, impersonatedUser);
      String json = GSON.create().toJson(new TroubleClient.CorrectionContainer(correction)); // serialize
      String url = String.format(FORMAT_GET_POST_CORRECTION, baseApiUrl, ticketId);
      String content = sendJsonRequest(url, apiUser, apiPass, "POST", json);
      correction = GSON.create().fromJson(content, TroubleClient.Correction.class);
    }
    correction.username = impersonatedUser; // set the acting user
    assert correction.id != null;

    // create new Package objects that only have the package_id field set.
    correction.packages = new TroubleClient.Package[] {new TroubleClient.Package(packageId)};

    String url = String.format(FORMAT_PUT_DELETE_CORRECTION, baseApiUrl, ticketId, correction.id);
    correction.id = null; // do not serialize!
    String json = GSON.create().toJson(new TroubleClient.CorrectionContainer(correction)); // serialize
    sendJsonRequest(url, apiUser, apiPass, "PUT", json);
  }

  /**
   * Delete a package in Trouble.
   */
  public void deletePackageFromFix(final String targetBranch, final int packageId) throws IOException {
    TroubleClient.Correction correction = getCorrection(targetBranch);
    if (correction == null) {
      LOG.debug("Correction for target branch {} does not exist", targetBranch);
      return;
    }

    String url = String.format(FORMAT_DELETE_PACKAGE_FROM_CORRECTION, baseApiUrl, ticketId, correction.id, packageId)
      + "?username=" + urlEncode(impersonatedUser);
    try {
      deleteRequest(url, apiUser, apiPass);
    } catch (TroubleClient.HttpException e) {
      if (e.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        throw e;
      }
      LOG.debug("Package " + packageId + " already deleted in ticket " + ticketId);
    }
  }

  /**
   * Creates the Title of a Gerrit correction info.
   */
  private static String correctionTitle(final String targetBranch) {
    return "Gerrit fix for " + targetBranch;
  }

  /**
   * Generic GET request.
   */
  private static String getRequest(final String api, final String user, final String pass) throws IOException {
    LOG.info(">> GET {}", api);
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      LOG.info("<< {} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
        String body = readAll(conn.getErrorStream());
        LOG.info("<< {}", body);
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage(), body);
      }
      String resp = readAll(conn.getInputStream());
      LOG.debug("<< {}", resp);
      return resp;
    } finally {
      conn.disconnect();
    }
  }

  /**
   * Generic PUT or POST request for JSON APIs.
   */
  private static String sendJsonRequest(final String api, final String user, final String pass,
      final String method, final String json) throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.info(">> {} {}", method, api);
      LOG.debug(">> {}", json);
    }
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      conn.setRequestMethod(method);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("charset", "utf-8");
      byte[] content = getBytes(json);
      conn.setRequestProperty("Content-Length", "" + Integer.toString(content.length));
      writeAll(conn.getOutputStream(), content);
      LOG.info("<< {} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() !=  HttpURLConnection.HTTP_OK && conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
        String body = readAll(conn.getErrorStream());
        LOG.info("<< {}", body);
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage(), body);
      }
      String resp = readAll(conn.getInputStream());
      LOG.debug("<< {}", resp);
      return resp;
    } finally {
      conn.disconnect();
    }
  }

  /**
   * Generic DELETE request.
   */
  private static String deleteRequest(final String api, final String user, final String pass) throws IOException {
    LOG.info(">> DELETE {}", api);
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      conn.setRequestMethod("DELETE");
      LOG.info("<< {} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() !=  HttpURLConnection.HTTP_OK) {
        String body = readAll(conn.getErrorStream());
        LOG.info("<< {}", body);
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage(), body);
      }
      String resp = readAll(conn.getInputStream());
      LOG.debug("<< {}", resp);
      return resp;
    } finally {
      conn.disconnect();
    }
  }

  /**
   * Create a new HttpURLConnection for HTTP requests.
   */
  private static HttpURLConnection openConnection(final String api, final String user, final String pass) throws IOException {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(api);
      String proto = url.getProtocol();
      assert "https".equals(proto) || "http".equals(proto);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestProperty("Authorization", authHeader(user, pass)); // add the authorization credentials
      conn.setConnectTimeout(TIMEOUT_CONNECT_MILLIS); // dont wait forever
      conn.setReadTimeout(TIMEOUT_READ_MILLIS); // dont wait forever
    } catch (MalformedURLException e) {
      throw new AssertionError("Invalid Trouble API URL encountered: " + api, e);
    }
    return conn;
  }

  /**
   * Generate a valid Authenticate header value for Basic (preemtive) authentication.
   */
  private static String authHeader(final String user, final String pass) {
    return "Basic " + base64(user + ":" + pass);
  }

  /**
   * Base 64 encode the given string.
   */
  private static String base64(final String val) {
    return DatatypeConverter.printBase64Binary(getBytes(val));
  }

  /**
   * Read all data from the given stream.
   *
   * The stream is closed by this method.
   */
  private static String readAll(final InputStream is) {
    return readAll(is, true);
  }

  /**
   * Read all data from the given stream.
   */
  private static String readAll(final InputStream is, final boolean close) {
    if (is == null) {
      return "";
    }
    try {
      Scanner scanner = new java.util.Scanner(is, "utf-8").useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    } finally {
      if (close) {
        try { is.close(); } catch (IOException e) { /* ignore errors on close */ }
      }
    }
  }

  /**
   * Write the byte array to stream.
   *
   * The stream is closed by this method.
   */
  private static void writeAll(final OutputStream os, final byte[] data) throws IOException {
    writeAll(os, data, true);
  }

  /**
   * Write the byte array to stream.
   */
  private static void writeAll(final OutputStream os, final byte[] data, final boolean close) throws IOException {
    try {
      os.write(data);
    } finally {
      if (close) {
        try { os.close(); } catch (IOException e) { /* ignore errors on close */ }
      }
    }
  }

  /**
   * Get bytes in a safe (FindBugs approved) manner.
   */
  private static byte[] getBytes(final String str) {
    try {
      return str.getBytes("utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is not supported on this machine", e);
    }
  }

  /**
   * URL encodes the given string.
   */
  private static String urlEncode(final String str) {
    try {
      return URLEncoder.encode(str, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 is not supported on this machine", e);
    }
  }

}
