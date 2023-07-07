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
package influent.midtier.spi.impl;

import influent.idl.FL_Entity;
import influent.idl.FL_Property;
import influent.idl.FL_PropertyTag;
import influent.idlhelper.EntityHelper;
import influent.idlhelper.PropertyHelper;
import influent.midtier.kiva.data.KivaTypes;
import influent.server.dataaccess.DataAccessException;
import influent.server.spi.impl.GenericEntityPropertiesView;
import influent.server.utilities.TypedId;

import java.awt.Dimension;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;



public class KivaAccountPropertiesView extends GenericEntityPropertiesView {
	
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd MMM yyyy");
	private static final String BASE_URL = "http://www.kiva.org/img/";
	private static final String DEFAULT_URL = "img/kiva_default.png";

	@Override
	protected String getImageUrl(FL_Entity entity, int width, int height){
		// Get the image id.
		String imageId = getProperty("image_id", entity);

		// If a non-valid, zero id is returned, use the default image.
		if (imageId.isEmpty() || imageId.equals("0")){
			return DEFAULT_URL;
		}
		return BASE_URL + "w" + width + "/" + imageId + ".jpg";
	}

	@Override
	protected Dimension getImageBoxSize() {
		return new Dimension(160, 100);
	}

	protected String getProperty(String propertyName, FL_Entity entity){
		for (FL_Property prop : entity.getProperties()) {
			PropertyHelper p = PropertyHelper.from(prop);
			if (p.getKey().toString().equals(propertyName)) {
				return p.getValue().toString();
			}
		}
		return new String();
	}
	
	@Override
	public String getContent(FL_Entity entity) throws DataAccessException {
		String id = TypedId.fromTypedId(entity.getUid().toString()).getNativeId();
		String type = getProperty("type",entity);
		String label = EntityHelper.getFirstPropertyByTag(entity, FL_PropertyTag.NAME).getValue().toString();
		String location = "";
		//Construct the HTML.
		StringBuilder html = new StringBuilder();		
		String imageUrl = getImageUrl(entity, 200, 150);

		if (type.equalsIgnoreCase(KivaTypes.TYPE_LOAN)){
			// Based on the entity type, construct the appropriate html content.
			Boolean delinquent = Boolean.parseBoolean(getProperty("loans_delinquent", entity));
			
			String descriptionEN = getProperty("loans_description_texts_en", entity);
			String descriptionRU = getProperty("loans_description_texts_ru", entity);
			String descriptionFR = getProperty("loans_description_texts_fr", entity);
			String descriptionES = getProperty("loans_description_texts_es", entity);
			String descriptionVI = getProperty("loans_description_texts_vi", entity);
			String descriptionID = getProperty("loans_description_texts_id", entity);
			String descriptionPT = getProperty("loans_description_texts_pt", entity);
			String descriptionMN = getProperty("loans_description_texts_mn", entity);
			String descriptionAR = getProperty("loans_description_texts_ar", entity);
			
			String dispursalAmount = getProperty("loans_terms_dispursalAmount", entity);
			String dispursalCurrency = getProperty("loans_terms_dispursalCurrency", entity);
			
			String fundedDate  = getFormattedDate(entity, "loans_fundedDate");
			String paidDate = getFormattedDate(entity, "loans_paidDate");
			
			String locationCC = getProperty("loans_location_countryCode", entity);
			String locationTown = getProperty("loans_location_town", entity);
			
			String sector = getProperty("loans_sector", entity);
			String status = getProperty("loans_status", entity);
			String use = getProperty("loans_use", entity);
			
			String kivaUrl = "www.kiva.org/lend/" + id.substring(1);
			
			String youtubeId = getProperty("loans_video_youtubeId", entity);
			if (youtubeId != null && !youtubeId.isEmpty()) {
				youtubeId = "www.youtube.com/watch?v=" + youtubeId;
			} else {
				youtubeId = null;
			}
			
			NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
			String loanAmount = nf.format(new BigDecimal(getProperty("loans_loanAmount", entity)));
			String paidAmount = nf.format(new BigDecimal(getProperty("loans_paidAmount", entity)));

			if (dispursalAmount != null && !dispursalAmount.isEmpty() && dispursalCurrency != null && !dispursalCurrency.isEmpty()) {
				loanAmount = dispursalAmount + " " + dispursalCurrency + " (" + loanAmount + ")";
			}
			
			appendDetailsHeader(html, label, "<br>" + locationTown + ", " + locationCC + (delinquent ? "<br>Loan Delinquent" : ""), "rest/icon/aperture-hscb/Person?iconWidth=32&amp;iconHeight=32", imageUrl, getImageBoxSize());

			html.append("<div id='detailsBody'>");
			html.append("	<table class='propertyTable' style='bottom: 2px; left: 0px;'><tbody>");
			appendPropertyTableRow(html, "Sector", sector);
			appendPropertyTableRow(html, "Loan Amount", loanAmount);
			appendPropertyTableRow(html, "Loan Use", use);
			appendPropertyTableRow(html, "Funded Date", fundedDate);
			appendPropertyTableRow(html, "Loan Status", status);
			appendPropertyTableRow(html, "Paid Amount", paidAmount);
			appendPropertyTableRow(html, "Paid Date", paidDate);
			appendPropertyTableRow(html, "Kiva URL", "<a href='http://" + kivaUrl + "' target='_blank'>" + kivaUrl);
			if (youtubeId != null) {
				appendPropertyTableRow(html, "YouTube Video", "<a href='http://" + youtubeId + "' target='_blank'>"+ youtubeId);
			}
			if (descriptionAR != null && !descriptionAR.isEmpty()) {
				appendPropertyTableRow(html, "Description (AR)", descriptionAR);
			}
			if (descriptionMN != null && !descriptionMN.isEmpty()) {
				appendPropertyTableRow(html, "Description (MN)", descriptionMN);
			}
			if (descriptionPT != null && !descriptionPT.isEmpty()) {
				appendPropertyTableRow(html, "Description (PT)", descriptionPT);
			}
			if (descriptionID != null && !descriptionID.isEmpty()) {
				appendPropertyTableRow(html, "Description (ID)", descriptionID);
			}
			if (descriptionVI != null && !descriptionVI.isEmpty()) {
				appendPropertyTableRow(html, "Description (VI)", descriptionVI);
			}
			if (descriptionES != null && !descriptionES.isEmpty()) {
				appendPropertyTableRow(html, "Description (ES)", descriptionES);
			}
			if (descriptionFR != null && !descriptionFR.isEmpty()) {
				appendPropertyTableRow(html, "Description (FR)", descriptionFR);
			}
			if (descriptionRU != null && !descriptionRU.isEmpty()) {
				appendPropertyTableRow(html, "Description (RU)", descriptionRU);
			}
			if (descriptionEN != null && !descriptionEN.isEmpty()) {
				appendPropertyTableRow(html, "Description (EN)", descriptionEN);
			}
			html.append("	</tbody></table>");
			html.append("</div>");
		}

		else if (type.equalsIgnoreCase(KivaTypes.TYPE_PARTNER)){

			String status = getProperty("partners_status", entity);
			String dueDiligenceType = getProperty("partners_dueDiligenceType", entity);
			
			String startDate  = getFormattedDate(entity, "partners_startDate");
			NumberFormat pf = NumberFormat.getPercentInstance(Locale.US);
			pf.setMinimumFractionDigits(2);
			
			Double delinquencyRateDouble = Double.parseDouble(getProperty("partners_delinquencyRate", entity)) / 100.0;
			String delinquencyRate = pf.format(delinquencyRateDouble);
			
			Double defaultRateDouble = Double.parseDouble(getProperty("partners_defaultRate", entity)) / 100.0;
			String defaultRate = pf.format(defaultRateDouble);
			
			NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
			String totalAmountRaised = cf.format(new BigDecimal(getProperty("partners_totalAmountRaised", entity)));
			
			String loansPosted = getProperty("partners_loansPosted", entity);
			
			String urlId = id;
			if (urlId.indexOf('-')>0) {
				urlId = urlId.substring(0, urlId.indexOf('-'));
			}
			
			String kivaUrl = "www.kiva.org/partners/" + urlId.substring(1);
			
			appendDetailsHeader(html, label, null, "rest/icon/aperture-hscb/Organization?iconWidth=32&amp;iconHeight=32&amp;role=business", imageUrl, getImageBoxSize());
			
			html.append("<div id='detailsBody'>");
			html.append("	<table class='propertyTable' style='bottom: 2px; left: 0px;'><tbody>");
			appendPropertyTableRow(html, "Operating Since", startDate);
			appendPropertyTableRow(html, "Status", status);
			appendPropertyTableRow(html, "Rating", startDate);
			appendPropertyTableRow(html, "Total Amount Raised", totalAmountRaised);
			appendPropertyTableRow(html, "Loans Posted", loansPosted);
			appendPropertyTableRow(html, "Due Diligence Type", dueDiligenceType);
			appendPropertyTableRow(html, "Delinquency Rate", delinquencyRate);
			appendPropertyTableRow(html, "Default Rate", defaultRate);
			appendPropertyTableRow(html, "Kiva URL", "<a href='http://" + kivaUrl + "' target='_blank'>" + kivaUrl);
			html.append("	</tbody></table>");
			html.append("</div>");
		}

		else if (type.equalsIgnoreCase(KivaTypes.TYPE_LENDER)){
			location = getProperty("lenders_whereabouts", entity);
			
			String because = getProperty("lenders_loanBecause", entity);
			String country = getProperty("lenders_countryCode", entity);
			String inviteeCount = getProperty("lenders_inviteeCount", entity);
			String loanCount = getProperty("lenders_loanCount", entity);

			String memberSince  = getFormattedDate(entity, "lenders_memberSince");
			String occupation = getProperty("lenders_occupation", entity);
			String occupationInfo = getProperty("lenders_occupationalInfo", entity);
			String personalUrl = getProperty("lenders_personalUrl", entity);
			
			String kivaUrl = "www.kiva.org/lender/" + id.substring(1);
			
			String teams = getProperty("lender_teams",entity);
			List<String> teamList = null;
			if (teams!=null) { 
				 teamList = Arrays.asList(teams.split(","));
			}
			
			// Make this an absolute address if it isn't already one.
			String hrefValue = !personalUrl.startsWith("http://")?"http://" + personalUrl:personalUrl;
			String postLabelStr = "<br>" + location + (country.isEmpty()?"":", " + country);
			
			appendDetailsHeader(html, label, postLabelStr, "rest/icon/aperture-hscb/Person?iconWidth=32&amp;iconHeight=32&amp;role=business", imageUrl, getImageBoxSize());
			
			html.append("<div id='detailsBody'>");
			html.append("	<table class='propertyTable' style='bottom: 2px; left: 0px;'><tbody>");
			appendPropertyTableRow(html, "Member Since", memberSince);
			appendPropertyTableRow(html, "Loan Count", loanCount);
			appendPropertyTableRow(html, "Occupation", occupation);
			appendPropertyTableRow(html, "Occupational Info", occupationInfo);
			appendPropertyTableRow(html, "Invite Count", inviteeCount);
			appendPropertyTableRow(html, "Kiva URL", "<a href='http://" + kivaUrl + "' target='_blank'>" + kivaUrl);
			if (personalUrl != null && !personalUrl.isEmpty()) {
				appendPropertyTableRow(html, "Personal URL", "<a href='" + hrefValue + "' target='_blank'>" + personalUrl);
			}
			appendPropertyTableRow(html, "Loan Because", because);
			
			if (teamList != null && teamList.size() == 1) {
				appendPropertyTableRow(html, "Team", teamList.get(0));
			} else if (teamList != null && teamList.size()>1) {
				appendPropertyTableRow(html, "Teams", teamList.get(0));
				for (int i=1;i<teamList.size();i++) {		//Skip first team
					appendPropertyTableRow(html, "", teamList.get(i));
				}
			}

			html.append("	</tbody></table>");
			html.append("</div>");
		}
		
		String str = html.toString();
		str = str.replace("\\r", "");
		str = str.replace("\\n", "<br>");
		
		return str;
	}
	
	private static void appendPropertyTableRow(StringBuilder sb, String propertyName, String propertyValue) {
		sb.append("<tr><td class='propertyName'>");
		sb.append(propertyName);
		sb.append(":</td><td class='propertyValue'>");
		sb.append(propertyValue);
		sb.append("</td></tr>");
	}
	
	private static void appendDetailsHeader(StringBuilder sb, String label, String postLabelStr, String iconUrl, String imageUrl, Dimension imageDim) {
		sb.append("<div style='width: 100%; overflow: hidden;'>");
		sb.append("	<div id='detailsHeaderInfo'>");
		sb.append("		<div class='detailsIconContainer'>");
		sb.append("			<img src='" + iconUrl + "' width='32' height='32'>");
		sb.append("		</div>");
		sb.append("		<div class='textWrapNodeContainer' style='width: 75%; top: 37px; left: 0px;'>"); 
		sb.append("			<b>" + label + "</b>");
		
		if(postLabelStr != null) {
			sb.append(postLabelStr);
		}
		
		sb.append("		</div>");
		sb.append("	</div>");
		sb.append("	<div id='detailsHeaderPhoto' style='min-width:"+imageDim.width+"px; max-width:"+imageDim.width+"px; min-height:"+imageDim.height+"px; max-height:"+imageDim.height+"px;'>");
		sb.append(" 	<div style='float: right; clear: left;'>");
		sb.append("			<img src='" + imageUrl + "' style='image-rendering:optimizeQuality;'>");
		sb.append("		</div>");
		sb.append("	</div>");
		sb.append("</div>");
	}
	
	
	private String getFormattedDate(FL_Entity entity, String datePropKey) {
		String toReturn = getProperty(datePropKey, entity);
		
		if (toReturn != null && !toReturn.isEmpty()) {
			DateTime startDateDateTime = new DateTime(Long.parseLong(toReturn));
			toReturn = startDateDateTime.toString(DATE_FORMATTER);
		}
		
		return toReturn;
	}
}
