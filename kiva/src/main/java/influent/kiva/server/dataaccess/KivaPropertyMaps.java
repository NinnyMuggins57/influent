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

public class KivaPropertyMaps {
	public static KivaPropertyMaps INSTANCE = new KivaPropertyMaps();
	
	private Map<String,KivaPropertyMapping> lenderMap = new HashMap<String, KivaPropertyMapping>();
	private Map<String,KivaPropertyMapping> teamMap = new HashMap<String, KivaPropertyMapping>();
	private Map<String,KivaPropertyMapping> borrowersMap = new HashMap<String, KivaPropertyMapping>();
	private Map<String,KivaPropertyMapping> partnerMap = new HashMap<String, KivaPropertyMapping>();
	
	private Map<String,KivaPropertyMapping> allMap = new HashMap<String, KivaPropertyMapping>();
	
	private Map<String,String> lenderGeoMap = new HashMap<String,String>();
	private Map<String,String> teamGeoMap = new HashMap<String,String>();
	private Map<String,String> borrowersGeoMap = new HashMap<String,String>();
	
	private KivaPropertyMaps() {
		lenderMap.put("lenders_uid", new KivaPropertyMapping("UID", FL_PropertyTag.ID,FL_PropertyTag.RAW));
		lenderMap.put("lenders_lenderId", new KivaPropertyMapping("Lender ID", FL_PropertyTag.RAW));
		lenderMap.put("lenders_name", new KivaPropertyMapping("Name", FL_PropertyTag.NAME, FL_PropertyTag.RAW));
		lenderMap.put("lenders_image_id", new KivaPropertyMapping("Image ID", FL_PropertyTag.RAW));
		lenderMap.put("lenders_image_templateId", new KivaPropertyMapping("Image Template ID", FL_PropertyTag.RAW));
		lenderMap.put("lenders_memberSince", new KivaPropertyMapping("Member Since", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		lenderMap.put("lenders_whereabouts", new KivaPropertyMapping("Whereabouts", FL_PropertyTag.RAW));
		lenderMap.put("lenders_countryCode", new KivaPropertyMapping("Country Code", FL_PropertyTag.RAW));
		lenderMap.put("lenders_personalUrl", new KivaPropertyMapping("Personal URL", FL_PropertyTag.LINKED_DATA,FL_PropertyTag.RAW));
		lenderMap.put("lenders_occupation", new KivaPropertyMapping("Occupation", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		lenderMap.put("lenders_occupationalInfo", new KivaPropertyMapping("Occupational Info", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		lenderMap.put("lenders_inviterId", new KivaPropertyMapping("Inviter ID", FL_PropertyTag.RAW));
		lenderMap.put("lenders_inviteeCount", new KivaPropertyMapping("Inviter Count", FL_PropertyTag.RAW));
		lenderMap.put("lenders_loanCount", new KivaPropertyMapping("Loan Count", FL_PropertyTag.RAW));
		lenderMap.put("lenders_loanBecause", new KivaPropertyMapping("Loan Reason", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		lenderMap.put("lat", new KivaPropertyMapping("Latitude", FL_PropertyTag.RAW));
		lenderMap.put("lon", new KivaPropertyMapping("Longitude", FL_PropertyTag.RAW));
		
		lenderGeoMap.put("lenders_whereabouts", "text");
		lenderGeoMap.put("lenders_countryCode", "cc");
		lenderGeoMap.put("lat", "lat");
		lenderGeoMap.put("lon", "lon");

		teamMap.put("teams_id", new KivaPropertyMapping("ID", FL_PropertyTag.ID,FL_PropertyTag.RAW));
		teamMap.put("teams_shortname", new KivaPropertyMapping("Short Name", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		teamMap.put("teams_name", new KivaPropertyMapping("Name", FL_PropertyTag.NAME, FL_PropertyTag.LABEL,FL_PropertyTag.RAW));
		teamMap.put("teams_category", new KivaPropertyMapping("Category", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		teamMap.put("teams_image_id", new KivaPropertyMapping("Image ID", FL_PropertyTag.RAW));
		teamMap.put("teams_image_templateId", new KivaPropertyMapping("Image Template ID", FL_PropertyTag.RAW));
		teamMap.put("teams_whereabouts", new KivaPropertyMapping("Whereabouts", FL_PropertyTag.RAW));
		teamMap.put("teams_loanBecause", new KivaPropertyMapping("Loan Reason", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		teamMap.put("teams_description", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		teamMap.put("teams_websiteUrl", new KivaPropertyMapping("Website URL", FL_PropertyTag.LINKED_DATA,FL_PropertyTag.RAW));
		teamMap.put("teams_teamSince", new KivaPropertyMapping("Team Since", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		teamMap.put("teams_membershipType", new KivaPropertyMapping("Membership Type", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		teamMap.put("teams_memberCount", new KivaPropertyMapping("Member Count", FL_PropertyTag.RAW));
		teamMap.put("teams_loanCount", new KivaPropertyMapping("Loan Count", FL_PropertyTag.RAW));
		teamMap.put("teams_loanedAmount", new KivaPropertyMapping("Loan Amount", FL_PropertyTag.RAW));
		
		teamGeoMap.put("teams_whereabouts", "text");

		partnerMap.put("partners_id", new KivaPropertyMapping("ID", FL_PropertyTag.ID,FL_PropertyTag.RAW));
		partnerMap.put("partners_name", new KivaPropertyMapping("Name", FL_PropertyTag.NAME, FL_PropertyTag.LABEL,FL_PropertyTag.RAW));
		partnerMap.put("partners_status", new KivaPropertyMapping("Status", FL_PropertyTag.STATUS, FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		partnerMap.put("partners_rating", new KivaPropertyMapping("Rating", FL_PropertyTag.STAT,FL_PropertyTag.RAW));
		partnerMap.put("partners_dueDiligenceType", new KivaPropertyMapping("Due Diligence Type", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		partnerMap.put("partners_image_id", new KivaPropertyMapping("Image ID", FL_PropertyTag.RAW));
		partnerMap.put("partners_image_templateId", new KivaPropertyMapping("Image Template ID", FL_PropertyTag.RAW));
		partnerMap.put("partners_startDate", new KivaPropertyMapping("Start Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		partnerMap.put("partners_delinquencyRate", new KivaPropertyMapping("Delinquency Rate", FL_PropertyTag.STAT,FL_PropertyTag.RAW));
		partnerMap.put("partners_defaultRate", new KivaPropertyMapping("Default Rate", FL_PropertyTag.STAT,FL_PropertyTag.RAW));
		partnerMap.put("partners_totalAmountRaised", new KivaPropertyMapping("Total Amount Raised", FL_PropertyTag.RAW));
		partnerMap.put("partners_loansPosted", new KivaPropertyMapping("Loans Posted", FL_PropertyTag.RAW));
		
		borrowersMap.put("loans_id", new KivaPropertyMapping("ID", FL_PropertyTag.ID,FL_PropertyTag.RAW));
		borrowersMap.put("loans_partnerId", new KivaPropertyMapping("Partner ID", FL_PropertyTag.RAW));
		borrowersMap.put("loans_name", new KivaPropertyMapping("Name", FL_PropertyTag.NAME, FL_PropertyTag.RAW));
		borrowersMap.put("loans_use", new KivaPropertyMapping("Use", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_activity", new KivaPropertyMapping("Activity", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_sector", new KivaPropertyMapping("Sector", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_status", new KivaPropertyMapping("Status", FL_PropertyTag.STATUS, FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_loanAmount", new KivaPropertyMapping("Loan Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_fundedAmount", new KivaPropertyMapping("Funded Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_basketAmount", new KivaPropertyMapping("Basket Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_paidAmount", new KivaPropertyMapping("Paid Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_currencyExchangeLossAmount", new KivaPropertyMapping("Currency Exchange Loss Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_postedDate", new KivaPropertyMapping("Posted Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		borrowersMap.put("loans_paidDate", new KivaPropertyMapping("Paid Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		borrowersMap.put("loans_delinquent", new KivaPropertyMapping("Deliquent", FL_PropertyTag.RAW));
		borrowersMap.put("loans_fundedDate", new KivaPropertyMapping("Funded Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		borrowersMap.put("loans_plannedExpirationDate", new KivaPropertyMapping("Planned Expiration Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_en", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_ru", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_fr", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_es", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_vi", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_id", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_pt", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_mn", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_description_texts_ar", new KivaPropertyMapping("Description", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_image_id", new KivaPropertyMapping("Image ID", FL_PropertyTag.RAW));
		borrowersMap.put("loans_image_templateId", new KivaPropertyMapping("Image Template ID", FL_PropertyTag.RAW));
		borrowersMap.put("loans_video_id", new KivaPropertyMapping("Video ID", FL_PropertyTag.RAW));
		borrowersMap.put("loans_video_youtubeId", new KivaPropertyMapping("Video Youtube ID", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_geo_level", new KivaPropertyMapping("Geo Level", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_geo_pairs", new KivaPropertyMapping("Geo Pairs", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_geo_type", new KivaPropertyMapping("Geo Type", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_town", new KivaPropertyMapping("Town", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_countryCode", new KivaPropertyMapping("Country Code", FL_PropertyTag.RAW));
		borrowersMap.put("loans_location_country", new KivaPropertyMapping("Country", FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_loanAmount", new KivaPropertyMapping("Loan Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_disbursalDate", new KivaPropertyMapping("Disbursal Date", FL_PropertyTag.DATE,FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_disbursalCurrency", new KivaPropertyMapping("Disbursal Currency", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_disbursalAmount", new KivaPropertyMapping("Disbursal Amount", FL_PropertyTag.AMOUNT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_lossLiability_currencyExchange", new KivaPropertyMapping("Loss Liability Currency Exchange", FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_lossLiability_currencyExchangeCoverageRate", new KivaPropertyMapping("Loss Liability Coverage Rate", FL_PropertyTag.RAW));
		borrowersMap.put("loans_terms_lossLiability_nonpayment", new KivaPropertyMapping("Loss Liability Non-Payment", FL_PropertyTag.RAW));
		borrowersMap.put("loans_journalTotals_entries", new KivaPropertyMapping("Journal Entries", FL_PropertyTag.RAW));
		borrowersMap.put("loans_journalTotals_bulkEntries", new KivaPropertyMapping("Journal Bulk Entries", FL_PropertyTag.RAW));
		borrowersMap.put("lat", new KivaPropertyMapping("Latitude", FL_PropertyTag.RAW));
		borrowersMap.put("lon", new KivaPropertyMapping("Longitude", FL_PropertyTag.RAW));
		borrowersMap.put("loans_borrowers_lastName", new KivaPropertyMapping("Borrower's Last Name", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_borrowers_firstName", new KivaPropertyMapping("Borrower's First Name", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_borrowers_gender", new KivaPropertyMapping("Borrower's Gender", FL_PropertyTag.TEXT,FL_PropertyTag.RAW));
		borrowersMap.put("loans_borrowers_pictured", new KivaPropertyMapping("Borrowers Pictured", FL_PropertyTag.RAW));
		
		borrowersGeoMap.put("loans_location_town", "text");
		borrowersGeoMap.put("loans_location_countryCode", "cc");
		borrowersGeoMap.put("loans_location_country", "text");
		borrowersGeoMap.put("lat", "lat");
		borrowersGeoMap.put("lon", "lon");
		
		allMap.put("id", new KivaPropertyMapping("ID", FL_PropertyTag.ID,FL_PropertyTag.RAW));
        allMap.put("timestamp", new KivaPropertyMapping("Timestamp", FL_PropertyTag.DATE,FL_PropertyTag.RAW));

        allMap.putAll(borrowersMap);
		allMap.putAll(lenderMap);
		allMap.putAll(partnerMap);
		allMap.putAll(teamMap);
	}
	
	public Map<String, KivaPropertyMapping> getLenderMap() { return lenderMap; }
	public Map<String, KivaPropertyMapping> getTeamMap() { return teamMap; }
	public Map<String, KivaPropertyMapping> getPartnerMap() { return partnerMap; }
	public Map<String, KivaPropertyMapping> getBorrowerMap() { return borrowersMap; }
	public Map<String, KivaPropertyMapping> getPropMap() { return allMap; }
	
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
