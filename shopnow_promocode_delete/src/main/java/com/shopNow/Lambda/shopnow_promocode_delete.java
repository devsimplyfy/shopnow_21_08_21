package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONObject;

public class shopnow_promocode_delete implements RequestHandler<JSONObject, JSONObject> {

	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings({ "unchecked" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();
		logger.log("Invoked JDBCSample.getCurrentTime");
		JSONObject errorPayload = new JSONObject();

		if (!input.containsKey("user_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 480);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'user_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}
		if (!input.containsKey("promocode_id")) {
			errorPayload.put("errorType", "BadRequest");
			errorPayload.put("httpStatus", 480);
			errorPayload.put("requestId", context.getAwsRequestId());
			errorPayload.put("message", "JSON Input Object request key named 'promocode_id' is missing");
			throw new RuntimeException(errorPayload.toJSONString());
		}

//		JSONArray category_array = new JSONArray();
		Connection conn = null;
		JSONObject jsonObjectFinalResult = new JSONObject();

		Properties prop = new Properties();
		
		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));

		} catch (Exception e) {

			jsonObjectFinalResult.put("message", "property file not found");
			jsonObjectFinalResult.put("status", 0);
			return jsonObjectFinalResult;

		}

		int user_id = 0;
		int promocode_id = 0;

		if (input.get("user_id") != null && input.get("user_id") != "") {

			try {
				user_id = Integer.parseInt(input.get("user_id").toString());

			} catch (Exception e) {

				jsonObjectFinalResult.put("message", "user id passed is in incorrect format");
				jsonObjectFinalResult.put("status", 0);
				return jsonObjectFinalResult;

			}
		} else {
			jsonObjectFinalResult.put("message", "enter valid user id");
			jsonObjectFinalResult.put("status", 0);
			return jsonObjectFinalResult;

		}

		if (input.get("promocode_id") != null && input.get("promocode_id") != "") {
			if (Integer.parseInt(input.get("promocode_id").toString()) < 1) {
				jsonObjectFinalResult.put("message", "enter valid promocode id");
				jsonObjectFinalResult.put("status", 0);
				return jsonObjectFinalResult;
			} else {

				try {
					promocode_id = Integer.parseInt(input.get("promocode_id").toString());

				} catch (Exception e) {

					jsonObjectFinalResult.put("message", "promocode id passed is in incorrect format");
					jsonObjectFinalResult.put("status", 0);
					return jsonObjectFinalResult;

				}
			}
		} else {
			jsonObjectFinalResult.put("message", "enter valid promocode id");
			jsonObjectFinalResult.put("status", 0);
			return jsonObjectFinalResult;

		}

		// Get time from DB server
		try {

			final String key_url = prop.getProperty("key");
			final String initVector = prop.getProperty("initVector");

			String decodedString_url = System.getenv("url");
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key_url.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			byte[] decodedBytes_url = cipher.doFinal(Base64.getDecoder().decode(decodedString_url));

			String decodedString_username = System.getenv("username");
			byte[] decodedBytes_username = cipher.doFinal(Base64.getDecoder().decode(decodedString_username));

			String decodedString_password = System.getenv("password");
			byte[] decodedBytes_password = cipher.doFinal(Base64.getDecoder().decode(decodedString_password));

			DB_URL = new String(decodedBytes_url);
			USERNAME = new String(decodedBytes_username);
			PASSWORD = new String(decodedBytes_password);

		} catch (Exception e) {

			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			jo_catch.put("errorType", "Server Error");
			jo_catch.put("httpStatus", 500);
			jo_catch.put("requestId", context.getAwsRequestId());
			jo_catch.put("message", e.getMessage().toString());

			return jo_catch;

		}

		try {
			
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			try {
				logger.log("\n Database connection establishment starts for customer table");
				// logger.log("DB_URL\n "+ DB_URL);

				
				String sql = "SELECT * FROM customers where id =?";
				PreparedStatement p = conn.prepareStatement(sql);
				p.setInt(1, user_id);
				ResultSet resultSet = p.executeQuery();

				if (resultSet.next() == false) {
					jsonObjectFinalResult.put("message", "Enter valid user id");
					jsonObjectFinalResult.put("status", 0);
					return jsonObjectFinalResult;
				}

				else if (resultSet.getInt("id") != user_id && resultSet.getInt("email") != -1
						&& resultSet.getInt("password") != -1)

				{
					jsonObjectFinalResult.put("message", "Entered user id is not authorized to perform the operation");
					jsonObjectFinalResult.put("status", 0);
					return jsonObjectFinalResult;

				}
				resultSet.close();
				p.close();
				// conn.close();

			} catch (Exception e) {

				JSONObject jo_catch = new JSONObject();
				jo_catch.put("Exception", e.getMessage());
				jo_catch.put("errorType", "Server Error");
				jo_catch.put("httpStatus", 500);
				jo_catch.put("requestId", context.getAwsRequestId());
				jo_catch.put("message", e.getMessage().toString());

				return jo_catch;

			}

			try {
				logger.log("\n Database connection establishment starts for promocodes table");
				// logger.log("DB_URL\n "+ DB_URL);

			//	conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
				String sql = "SELECT * FROM promocodes where id =?";
				PreparedStatement p = conn.prepareStatement(sql);
				p.setInt(1, promocode_id);
				ResultSet resultSet = p.executeQuery();

				if (resultSet.next() == false) {
					jsonObjectFinalResult.put("message", "Enter valid promocode id");
					jsonObjectFinalResult.put("status", 0);
					return jsonObjectFinalResult;
				} else if (resultSet.getInt("status") == 0)

				{
					jsonObjectFinalResult.put("message", "promocode is already deleted");
					jsonObjectFinalResult.put("status", 0);
					return jsonObjectFinalResult;

				}
				resultSet.close();
				p.close();
				// conn.close();

			} catch (Exception e) {

				JSONObject jo_catch = new JSONObject();
				jo_catch.put("Exception", e.getMessage());
				jo_catch.put("errorType", "Server Error");
				jo_catch.put("httpStatus", 500);
				jo_catch.put("requestId", context.getAwsRequestId());
				jo_catch.put("message", e.getMessage().toString());

				return jo_catch;

			}

			PreparedStatement updatePromocodeRow = null;
			String queryForSqlUpdate;
			int result_count;

			logger.log("\n Database connection establishment starts for status updation inside promocodes table");
		//	conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
			queryForSqlUpdate = "UPDATE promocodes SET status=0 WHERE id=?";
			updatePromocodeRow = conn.prepareStatement(queryForSqlUpdate);
			updatePromocodeRow.setInt(1, promocode_id);

			try {

				result_count = updatePromocodeRow.executeUpdate();
			} catch (Exception e2) {

				JSONObject jo_catch = new JSONObject();
				jo_catch.put("Exception", e2.getMessage());
				jo_catch.put("errorType", "Server Error");
				jo_catch.put("httpStatus", 500);
				jo_catch.put("requestId", context.getAwsRequestId());
				jo_catch.put("message", e2.getMessage().toString());

				return jo_catch;
			}
			if (result_count > 0) {

				logger.log("\n updation performed successfully");

				jsonObjectFinalResult.put("message", "promocode is deleted successfully");
				jsonObjectFinalResult.put("status", 1);
				return jsonObjectFinalResult;
			} else {

				logger.log("\n Error occured while performing deletion");
				jsonObjectFinalResult.put("message", "Error occured while performing deletion");
				jsonObjectFinalResult.put("status", 0);

			}

		} catch (Exception e2) {

			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e2.getMessage());
			jo_catch.put("errorType", "Server Error");
			jo_catch.put("httpStatus", 500);
			jo_catch.put("requestId", context.getAwsRequestId());
			jo_catch.put("message", e2.getMessage().toString());

			return jo_catch;

		}

		finally {

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.log("Exception" + e.toString());
			}
		}
		//return the result
		return jsonObjectFinalResult;
		
	}

}
