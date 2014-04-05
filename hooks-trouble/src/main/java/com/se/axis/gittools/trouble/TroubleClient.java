/**
 * Copyright (C) 2014 Axis Communications AB
 */

package com.se.axis.gittools.trouble;

import org.eclipse.jgit.lib.Config;
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

  private static final Logger LOG = LoggerFactory.getLogger(TroubleClient.class);

  private final String baseApiUrl;
  private final String apiUser;
  private final String apiPass;

  private final String impersonatedUser;
  private final int ticketId;

  private final GsonBuilder gson = new GsonBuilder();

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
     * Constructor.
     */
    public HttpException(final int responseCode, final String responseMessage) {
      super(String.valueOf(responseCode) + " " + responseMessage);
      this.responseCode = responseCode;
      this.responseMessage = responseMessage;
    }
  }

  /**
   * Commit (SHA1) container.
   */
  public static class Reference {
    /**
     * Commit (SHA1).
     */
    @SerializedName("revision") public String reference;

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
  }

  /**
   * Package info.
   */
  public static class Package {
    /**
     * Trouble id of the package (used for updates).
     */
    public Integer id;

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
     */
    @SerializedName("assigned_username") public String assignedUsername;
  }

  /**
   * Package container.
   */
  public static class PackageContainer {
    /**
     * The contained package.
     */
    @SerializedName("package") public Package packageObj;

    /**
     * Default constructor for serialization.
     */
    public PackageContainer() {
    }

    /**
     * Helper constructor.
     */
    public PackageContainer(final Package packageObj) {
      this.packageObj = packageObj;
    }
  }

  /**
   * Comment info.
   */
  public static class Comment {
    /**
     * The comment free text.
     */
    public String text;

    /**
     * The impersonated user name.
     */
    public String username;

    /**
     * Default constructor for serialization.
     */
    public Comment() {
    }

    /**
     * Helper constructor.
     */
    public Comment(final String text) {
      this.text = text;
    }
  }

  /**
   * Comment container.
   */
  public static class CommentContainer {
    /**
     * A comment.
     */
    public Comment comment;

    /**
     * Default constructor for serialization.
     */
    public CommentContainer() {
    }

    /**
     * Helper constructor.
     */
    public CommentContainer(final Comment comment) {
      this.comment = comment;
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
  private TroubleClient(final Config gerritConfig, final int ticketId, final String impersonatedUser) {
    this.impersonatedUser = impersonatedUser; // The impersonated user
    this.ticketId = ticketId;

    String url = gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "url");
    assert  url != null;  // This needs to be checked earlier
    baseApiUrl =  url.replaceFirst("/+$", "");
    apiUser = gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "username");
    if (apiUser == null) {
      throw new IllegalArgumentException("trouble.username not set in gerrit.config");
    }
    apiPass = gerritConfig.getString(TroubleItsFacade.ITS_NAME_TROUBLE, null, "password");
    if (apiPass == null) {
      throw new IllegalArgumentException("trouble.password not set in secure.config");
    }
  }

  /**
   * Create a TroubleClient.
   */
  public static TroubleClient create(final Config gerritConfig, final int ticketId) {
    return create(gerritConfig, ticketId, null);
  }

  /**
   * Create a TroubleClient.
   */
  public static TroubleClient create(final Config gerritConfig, final int ticketId, final String impersonatedUser) {
    return new TroubleClient(gerritConfig, ticketId, impersonatedUser);
  }

  /**
   * Get ticket info from Trouble.
   */
  public TroubleClient.Ticket getTicket() throws IOException {
    String url = String.format(FORMAT_TICKET_GET, baseApiUrl, ticketId);
    TroubleClient.Ticket ticket = null;

    String content = getRequest(url, apiUser, apiPass);
    ticket = gson.create().fromJson(content, TroubleClient.Ticket.class); // deserialize!
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
  public void addComment(final TroubleClient.Comment comment) throws IOException {
    if (comment.username == null) {
      comment.username = impersonatedUser;
    }

    CommentContainer troubleComment = new CommentContainer(comment);
    String json = gson.create().toJson(troubleComment); // serialize
    String url = String.format(FORMAT_COMMENT_POST, baseApiUrl, ticketId);
    sendJsonRequest(url, apiUser, apiPass, "POST", json);
  }

  /**
   * Get a package from Trouble.
   */
  public TroubleClient.Package[] getPackages(final String name, final String targetBranch) throws IOException {
    StringBuilder url = new StringBuilder(String.format(FORMAT_GET_POST_PACKAGE, baseApiUrl, ticketId));
    if (name != null || targetBranch != null) {
      url.append('?');
      if (name != null) {
        url.append("package=").append(urlEncode(name)).append('&');
      }
      if (targetBranch != null) {
        url.append("target_branch=").append(urlEncode(targetBranch)).append('&');
      }
      url.setLength(url.length() - 1); // remove the trailing '&'
    }
    String content = getRequest(url.toString(), apiUser, apiPass);
    return gson.create().fromJson(content, TroubleClient.Package[].class); // deserialize!
  }

  /**
   * Create a package in Trouble.
   *
   * We cannot assume that the package is already added, since users can remove
   * it at any time. Hence we need to try update (PUT) first, and as a fallback,
   * we call add.
   */
  public void addOrUpdatePackage(final TroubleClient.Package packageObj) throws IOException {
    // See if the package has already been added
    if (packageObj.username == null) {
      packageObj.username = impersonatedUser;
    }
    PackageContainer troublePackage = new PackageContainer(packageObj);

    // get existing packages
    try { // PUT
      TroubleClient.Package[] existingPackages = getPackages(packageObj.name, packageObj.targetBranch);
      String json = gson.create().toJson(troublePackage); // serialize
      String url = String.format(FORMAT_PUT_DELETE_PACKAGE, baseApiUrl, ticketId, existingPackages[0].id);
      sendJsonRequest(url, apiUser, apiPass, "PUT", json);
    } catch (TroubleClient.HttpException e) { // POST
      if (e.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        throw e;
      }
      if (packageObj.assignedUsername == null) {
        packageObj.assignedUsername = impersonatedUser;
      }
      String json = gson.create().toJson(troublePackage); // serialize
      String url = String.format(FORMAT_GET_POST_PACKAGE, baseApiUrl, ticketId);
      sendJsonRequest(url, apiUser, apiPass, "POST", json);
    }
  }

  /**
   * Delete a package in Trouble.
   */
  public void deletePackage(final String name, final String targetBranch) throws IOException {
    // get existing packages
    int code = -1;
    try {
      TroubleClient.Package[] existingPackages = getPackages(name, targetBranch);
      String url = String.format(FORMAT_PUT_DELETE_PACKAGE, baseApiUrl, ticketId, existingPackages[0].id);
      deleteRequest(url, apiUser, apiPass);
    } catch (TroubleClient.HttpException e) {
      // See Ticket 61020 regarding HttpURLConnection.HTTP_INTERNAL_ERROR
      code = e.responseCode;
      if (code != HttpURLConnection.HTTP_NOT_FOUND && code != HttpURLConnection.HTTP_INTERNAL_ERROR) {
        throw e;
      }
      LOG.info("Package " + targetBranch + '@' + name + " already deleted in ticket " + ticketId);
    }
    if (code != HttpURLConnection.HTTP_INTERNAL_ERROR) {
      LOG.info("Ticket 61020 is fixed - remove workaround!");
    }
  }

  /**
   * Generic GET request.
   */
  private static String getRequest(final String api, final String user, final String pass) throws IOException {
    LOG.debug("GET {}", api);
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      LOG.debug("{} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage());
      }

      String resp = readAll(conn.getInputStream());
      LOG.debug("{}", resp);
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
    LOG.debug("{} {}", method, api);
    LOG.debug("{}", json);
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      conn.setRequestMethod(method);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("charset", "utf-8");
      byte[] body = getBytes(json);
      conn.setRequestProperty("Content-Length", "" + Integer.toString(body.length));
      writeAll(conn.getOutputStream(), body);
      LOG.debug("{} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() !=  HttpURLConnection.HTTP_OK && conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage());
      }

      String resp = readAll(conn.getInputStream());
      if (method.equals("POST")) {
        LOG.debug("{}", resp);
      }
      return resp;
    } finally {
      conn.disconnect();
    }
  }

  /**
   * Generic DELETE request.
   */
  private static String deleteRequest(final String api, final String user, final String pass) throws IOException {
    LOG.debug("DELETE {}", api);
    HttpURLConnection conn = openConnection(api, user, pass);
    try {
      conn.setRequestMethod("DELETE");
      LOG.debug("{} {}", conn.getResponseCode(), conn.getResponseMessage());
      if (conn.getResponseCode() !=  HttpURLConnection.HTTP_OK) {
        throw new TroubleClient.HttpException(conn.getResponseCode(), conn.getResponseMessage());
      }
      String resp = readAll(conn.getInputStream());
      LOG.debug("{}", resp);
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
