package com.server.stats;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;

import com.server.stats.meta.PlaceHolderMeta;

public class StatsAPIPlaceholderHandler {
    public static Map<String, Set<String>> emailZSIDSet = new HashMap<>();

    public static Function<PlaceHolderMeta, String> getPayoutPlaceholderHandler() {
        return (placeholderHandlerMeta) -> {

            return "test";

			//			JSONArray userDetails = (JSONArray) responseColumnValue;
//			int userDetailsLength = userDetails.length();
//
//			String emailID = null;
//			String zuid = null;
//			String name = null;
//			for(int i = 0; i < userDetailsLength; i++)
//			{
//				JSONObject userObject = userDetails.getJSONObject(i);
//
//				JSONArray integ = userObject.optJSONArray("integration_details");
//				if(integ == null)
//					continue;
//
//				for(int j = 0; j < integ.length(); j++)
//				{
//					if(integ.getJSONObject(j).getString("bank_name").equals("sbi_bank") && StringUtils.isNotEmpty(integ.getJSONObject(j).optString("identifier")))
//					{
//						return "true";
//					}
//				}
//
//			}
//
//			return "false";

//			JSONObject requestData = new JSONObject(responseColumnValue.toString());
//			return requestData.getJSONObject("beneMaintV2").getJSONArray("BeneList").getJSONObject(0).getString("BeneAccountNo");


            //			if(responseColumnName.equals("ISBIIntegrated"))
            //			{
            //				return emailID;
            //			}
            //			if(responseColumnName.equals("zuid"))
            //			{
            //				return zuid;
            //			}
            //			if(responseColumnName.equals("name"))
            //			{
            //				return name;
            //			}
        };
    }

    public static Function<PlaceHolderMeta, String> getBooksPlaceholderHandler() {
        return (placeholderHandlerMeta) -> {
            JSONObject response = new JSONObject(placeholderHandlerMeta.getColumnValue().toString());
            JSONArray jsonArray = response.getJSONArray("organization_details");
            String email = placeholderHandlerMeta.getRequestDataRow().get("${Col_0}");
            String orgs = "";
            for (int i = 0; i < jsonArray.length(); i++) {
                orgs += jsonArray.getJSONObject(i).getString("organization_id") + ",";
                try {
                    placeholderHandlerMeta.getStatsMeta().getPlaceHolderWriter().write(email + "," + jsonArray.getJSONObject(i).getString("organization_id") + "\n");
                    placeholderHandlerMeta.getStatsMeta().getPlaceHolderWriter().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return orgs;
        };
    }
}
