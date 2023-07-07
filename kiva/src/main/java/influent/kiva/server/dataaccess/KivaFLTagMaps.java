/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package influent.kiva.server.dataaccess;

import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idlhelper.PropertyHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KivaFLTagMaps {
	public static KivaFLTagMaps INSTANCE = new KivaFLTagMaps();
	
	private Map<String,FL_PropertyTag[]> lenderMap = new HashMap<String,FL_PropertyTag[]>(17);
	private Map<String,FL_PropertyTag[]> teamMap = new HashMap<String,FL_PropertyTag[]>(17);
	private Map<String,FL_PropertyTag[]> borrowersMap = new HashMap<String,FL_PropertyTag[]>(17);
	private Map<String,FL_PropertyTag[]> partnerMap = new HashMap<String,FL_PropertyTag[]>(17);
	
	private Map<String, FL_PropertyTag[]> allMap = new HashMap<String, FL_PropertyTag[]>();
	
	private Map<String,String> lenderGeoMap = new HashMap<String,String>(17);
	private Map<String,String> teamGeoMap = new HashMap<String,String>(17);
	private Map<String,String> borrowersGeoMap = new HashMap<String,String>(17);
	
	private KivaFLTagMaps() {
		lenderMap.put("lenders_uid", new FL_PropertyTag[]{FL_PropertyTag.ID,FL_PropertyTag.RAW } );
		lenderMap.put("lenders_lenderId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_name", new FL_PropertyTag[]{FL_PropertyTag.NAME, FL_PropertyTag.RAW });
		lenderMap.put("lenders_image_id", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_image_templateId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_memberSince", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		lenderMap.put("lenders_whereabouts", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_countryCode", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_personalUrl", new FL_PropertyTag[]{FL_PropertyTag.LINKED_DATA,FL_PropertyTag.RAW });
		lenderMap.put("lenders_occupation", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		lenderMap.put("lenders_occupationalInfo", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		lenderMap.put("lenders_inviterId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_inviteeCount", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_loanCount", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lenders_loanBecause", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		lenderMap.put("lat", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		lenderMap.put("lon", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		
		lenderGeoMap.put("lenders_whereabouts", "text");
		lenderGeoMap.put("lenders_countryCode", "cc");
		lenderGeoMap.put("lat", "lat");
		lenderGeoMap.put("lon", "lon");

		teamMap.put("teams_id", new FL_PropertyTag[]{FL_PropertyTag.ID,FL_PropertyTag.RAW });
		teamMap.put("teams_shortname", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		teamMap.put("teams_name", new FL_PropertyTag[]{FL_PropertyTag.NAME, FL_PropertyTag.LABEL,FL_PropertyTag.RAW });
		teamMap.put("teams_category", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		teamMap.put("teams_image_id", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		teamMap.put("teams_image_templateId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		teamMap.put("teams_whereabouts", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		teamMap.put("teams_loanBecause", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		teamMap.put("teams_description", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		teamMap.put("teams_websiteUrl", new FL_PropertyTag[]{FL_PropertyTag.LINKED_DATA,FL_PropertyTag.RAW });
		teamMap.put("teams_teamSince", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		teamMap.put("teams_membershipType", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		teamMap.put("teams_memberCount", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		teamMap.put("teams_loanCount", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		teamMap.put("teams_loanedAmount", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		
		teamGeoMap.put("teams_whereabouts", "text");

		partnerMap.put("partners_id", new FL_PropertyTag[]{FL_PropertyTag.ID,FL_PropertyTag.RAW });
		partnerMap.put("partners_name", new FL_PropertyTag[]{FL_PropertyTag.NAME, FL_PropertyTag.LABEL,FL_PropertyTag.RAW });
		partnerMap.put("partners_status", new FL_PropertyTag[]{FL_PropertyTag.STATUS, FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		partnerMap.put("partners_rating", new FL_PropertyTag[]{FL_PropertyTag.STAT,FL_PropertyTag.RAW });
		partnerMap.put("partners_dueDiligenceType", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		partnerMap.put("partners_image_id", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		partnerMap.put("partners_image_templateId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		partnerMap.put("partners_startDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		partnerMap.put("partners_delinquencyRate", new FL_PropertyTag[]{FL_PropertyTag.STAT,FL_PropertyTag.RAW });
		partnerMap.put("partners_defaultRate", new FL_PropertyTag[]{FL_PropertyTag.STAT,FL_PropertyTag.RAW });
		partnerMap.put("partners_totalAmountRaised", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		partnerMap.put("partners_loansPosted", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		
		borrowersMap.put("loans_id", new FL_PropertyTag[]{FL_PropertyTag.ID,FL_PropertyTag.RAW });
		borrowersMap.put("loans_partnerId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_name", new FL_PropertyTag[]{FL_PropertyTag.NAME, FL_PropertyTag.RAW });
		borrowersMap.put("loans_use", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_activity", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_sector", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_status", new FL_PropertyTag[]{FL_PropertyTag.STATUS, FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_loanAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_fundedAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_basketAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_paidAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_currencyExchangeLossAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_postedDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		borrowersMap.put("loans_paidDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		borrowersMap.put("loans_delinquent", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_fundedDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		borrowersMap.put("loans_plannedExpirationDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_en", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_ru", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_fr", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_es", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_vi", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_id", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_pt", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_mn", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_description_texts_ar", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_image_id", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_image_templateId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_video_id", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_video_youtubeId", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_geo_level", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_geo_pairs", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_geo_type", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_town", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_countryCode", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_location_country", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_loanAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_disbursalDate", new FL_PropertyTag[]{FL_PropertyTag.DATE,FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_disbursalCurrency", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_disbursalAmount", new FL_PropertyTag[]{FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_lossLiability_currencyExchange", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_lossLiability_currencyExchangeCoverageRate", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_terms_lossLiability_nonpayment", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_journalTotals_entries", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_journalTotals_bulkEntries", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("lat", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("lon", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		borrowersMap.put("loans_borrowers_lastName", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_borrowers_firstName", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_borrowers_gender", new FL_PropertyTag[]{FL_PropertyTag.TEXT,FL_PropertyTag.RAW });
		borrowersMap.put("loans_borrowers_pictured", new FL_PropertyTag[]{FL_PropertyTag.RAW });
		
		borrowersGeoMap.put("loans_location_town", "text");
		borrowersGeoMap.put("loans_location_countryCode", "cc");
		borrowersGeoMap.put("loans_location_country", "text");
		borrowersGeoMap.put("lat", "lat");
		borrowersGeoMap.put("lon", "lon");
		
		allMap.put("id", new FL_PropertyTag[]{FL_PropertyTag.ID,FL_PropertyTag.RAW });
		allMap.putAll(borrowersMap);
		allMap.putAll(lenderMap);
		allMap.putAll(partnerMap);
		allMap.putAll(teamMap);
	}
	
	public Map<String, FL_PropertyTag[]> getLenderMap() { return lenderMap; }
	public Map<String, FL_PropertyTag[]> getTeamMap() { return teamMap; }
	public Map<String, FL_PropertyTag[]> getPartnerMap() { return partnerMap; }
	public Map<String, FL_PropertyTag[]> getBorrowerMap() { return borrowersMap; }
	public Map<String, FL_PropertyTag[]> getPropTagMap() { return allMap; }
	
	Map<String, String> getLenderGeoFields() { return lenderGeoMap; }
	Map<String, String> getTeamGeoFields() { return teamGeoMap; }
	Map<String, String> getPartnerGeoFields() { Map<String,String> map = Collections.emptyMap(); return map; }
	Map<String, String> getBorrowerGeoFields() { return borrowersGeoMap; }
	
	public void appendPartnerProperties(List<FL_Property> props) {
		final FL_Property delinquency = PropertyHelper.getPropertyByKey(props, "partners_delinquencyRate");
		
		if (delinquency != null) {
			final Number number = (Number) PropertyHelper.from(delinquency).getValue();
			
			if (number != null && number.doubleValue() >= 5.0) {
				props.add(new PropertyHelper(FL_PropertyTag.WARNING, "high delinquency rate"));
			}
		}
		
		final FL_Property defaultRate = PropertyHelper.getPropertyByKey(props, "partners_defaultRate");
		
		if (defaultRate != null) {
			final Number number = (Number) PropertyHelper.from(defaultRate).getValue();
			
			if (number != null && number.doubleValue() >= 1.0) {
				props.add(new PropertyHelper(FL_PropertyTag.WARNING, "high default rate"));
			}
		}
	}
}
