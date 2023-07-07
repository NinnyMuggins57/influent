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
package influent.server.dataaccess;

import influent.idl.FL_DateInterval;
import influent.idl.FL_DateRange;
import influent.idl.FL_DirectionFilter;
import influent.idl.FL_LinkEntityTypeFilter;
import influent.server.dataaccess.AbstractDataNamespaceHandler.ID_TYPE;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.RandomAccess;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DataAccessHelper {

	// --- Flow ---
	public static final String FLOW_TABLE 									= "FinFlow";
	
	public static final String FLOW_COLUMN_FROM_ENTITY_ID 					= "FinFlow_FromEntityId";
	public static final String FLOW_COLUMN_FROM_ENTITY_TYPE 				= "FinFlow_FromEntityType";
	public static final String FLOW_COLUMN_TO_ENTITY_ID 					= "FinFlow_ToEntityId";
	public static final String FLOW_COLUMN_TO_ENTITY_TYPE 					= "FinFlow_ToEntityType";
	public static final String FLOW_COLUMN_AMOUNT 							= "FinFlow_Amount";
	public static final String FLOW_COLUMN_PERIOD_DATE						= "FinFlow_PeriodDate";

    // --- Entity ---
	public static final String ENTITY_TABLE									= "FinEntity";
	
	public static final String ENTITY_COLUMN_ENTITY_ID 						= "FinEntity_EntityId";
	public static final String ENTITY_COLUMN_INBOUND_AMOUNT 				= "FinEntity_InboundAmount";
	public static final String ENTITY_COLUMN_INBOUND_DEGREE					= "FinEntity_InboundDegree";
	public static final String ENTITY_COLUMN_UNIQUE_INBOUND_DEGREE 			= "FinEntity_UniqueInboundDegree";
	public static final String ENTITY_COLUMN_OUTBOUND_AMOUNT 				= "FinEntity_OutboundAmount";
	public static final String ENTITY_COLUMN_OUTBOUND_DEGREE 				= "FinEntity_OutboundDegree";
	public static final String ENTITY_COLUMN_UNIQUE_OUTBOUND_DEGREE 		= "FinEntity_UniqueOutboundDegree";
	public static final String ENTITY_COLUMN_BALANCE 						= "FinEntity_Balance";
	public static final String ENTITY_COLUMN_PERIOD_DATE					= "FinEntity_PeriodDate";
	
    // --- Cluster Summary ---
	public static final String CLUSTER_SUMMARY_TABLE 						= "ClusterSummary";

	public static final String CLUSTER_SUMMARY_COLUMN_ENTITY_ID 			= "ClusterSummary_EntityId";
	public static final String CLUSTER_SUMMARY_COLUMN_SUMMARY_ID 			= "ClusterSummary_SummaryId";
	public static final String CLUSTER_SUMMARY_COLUMN_PROPERTY 				= "ClusterSummary_Property";
	public static final String CLUSTER_SUMMARY_COLUMN_TAG					= "ClusterSummary_Tag";
	public static final String CLUSTER_SUMMARY_COLUMN_TYPE 					= "ClusterSummary_Type";
	public static final String CLUSTER_SUMMARY_COLUMN_VALUE 				= "ClusterSummary_Value";
	public static final String CLUSTER_SUMMARY_COLUMN_STAT 					= "ClusterSummary_Stat";

    // --- Cluster Summary Members ---
	public static final String CLUSTER_SUMMARY_MEMBERS_TABLE 				= "ClusterSummaryMembers";
	public static final String CLUSTER_SUMMARY_MEMBERS_COLUMN_ENTITY_ID 	= "ClusterSummaryMembers_EntityId";
	
	// --- Deprecated ---
	public static final String GLOBAL_CLUSTER_TABLE 						= "global_cluster_dataview";
	public static final String DYNAMIC_CLUSTER_TABLE						= "dynamic_cluster_dataview";
	
	public static final String CLIENT_STATE_TABLE 							= "clientState";
	
	
	
	// Serves no purpose other than for us to identify popup requests down the line when we are looking up entities.
	public interface DetailsSubject {}
	
	
	
	
	public static List<String> detailsSubject(String id) {
		return new DetailsSubjectList(id);
	}
	
	
	
	
    private static class DetailsSubjectList extends AbstractList<String> implements DetailsSubject, RandomAccess, Serializable {
        
    	static final long serialVersionUID = 3093736618740652951L;
        private final String _id;

        DetailsSubjectList(String id) {
        	_id = id;
        }

        public int size() {
        	return 1;
        }

        public boolean contains(Object obj) {
        	return _id.equals(obj);
        }

        public String get(int index) {
            if (index != 0) throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
            
            return _id;
        }
    }
    
    
    
    
	/**
	 * Formats a date, exclusive of time, as a UTC date string.
	 */
	public static String format(Long date) {
		if (date == null) {
			return null;
		}
		
		return format(new DateTime((long)date, DateTimeZone.UTC));
	}
	
	
	
	
	private static void pad00(
		int n, 
		StringBuilder s
	) {
		
		if (n < 10) {
			s.append('0');
		}
		s.append(n);
	}
	
	
	
	
	public static String format(DateTime dateTime) {
		
		if (dateTime == null) {
			return null;
		}
		
		StringBuilder s = new StringBuilder(10);
		
		s.append(dateTime.getYear());
		s.append('-');
		
		pad00(dateTime.getMonthOfYear(), s);
		s.append('-');
		
		pad00(dateTime.getDayOfMonth(), s);
		s.append(' ');

		pad00(dateTime.getHourOfDay(), s);
		s.append(':');
		
		pad00(dateTime.getMinuteOfHour(), s);
		s.append(':');
		
		pad00(dateTime.getSecondOfMinute(), s);
		s.append('.');
		
		int ms = dateTime.getMillisOfSecond();

		if (ms < 100) {
			s.append('0');
		}
		pad00(ms, s);
		
		return s.toString();
	}
	
	
	
	
	public static DateTime getStartDate(FL_DateRange date) {
		
		if (date == null || date.getStartDate() == null) {
			return null;
		}
		
		return new DateTime((long)date.getStartDate(), DateTimeZone.UTC);
	}
	
	
	
	
	public static DateTime getExclusiveEndDate(FL_DateRange date) {
		
		if (date == null) {
			return null;
		}

		DateTime d = new DateTime((long)date.getStartDate(), DateTimeZone.UTC);

		switch (date.getDurationPerBin().getInterval()) {
		case SECONDS:
			return d.plusSeconds(date.getNumBins().intValue());
		case HOURS:
			return d.plusHours(date.getNumBins().intValue());
		case DAYS:
			return d.plusDays(date.getNumBins().intValue());
		case WEEKS:
			return d.plusWeeks(date.getNumBins().intValue());
		case MONTHS:
			return d.plusMonths(date.getNumBins().intValue());
		case QUARTERS:
			return d.plusMonths(date.getNumBins().intValue() * 3);
		case YEARS:
			return d.plusYears(date.getNumBins().intValue());
		}
		
		return d;
	}
	
	
	
	
	/**
	 * Gets the inclusive end date for SQL between. 
	 */
	public static DateTime getEndDate(FL_DateRange date) {
		
		if (date == null) {
			return null;
		}

		// max millisecond precision in SQL datetime is .997
		return getExclusiveEndDate(date).minusMillis(3);
	}
	
	
	
	
	public static List<Date> getDateIntervals(FL_DateRange date) {
		
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(date.getStartDate());
		
		List<Date> dates = new ArrayList<Date>(date.getNumBins().intValue());
		for (int i = 0; i < date.getNumBins().intValue(); i++) {
			dates.add(c.getTime());
			switch (date.getDurationPerBin().getInterval()) {
				case SECONDS:
					c.add(Calendar.SECOND, 1);
					break;
				case HOURS:
					c.add(Calendar.HOUR_OF_DAY, 1);
					break;
				case DAYS:
					c.add(Calendar.DATE, 1);
					break;
				case WEEKS:
					c.add(Calendar.DATE, 7);
					break;
				case MONTHS:
					c.add(Calendar.MONTH, 1);
					break;
				case QUARTERS:
					c.add(Calendar.MONTH, 3);
					break;
				case YEARS:
					c.add(Calendar.YEAR, 1);
					break;
			}
		}
		
		return dates;
	}
	
	
	
	
	public static boolean isDateInRange(long date, FL_DateRange range) {
		return range.getStartDate() <= date && date <= getEndDate(range).getMillis();
	}
	
	
	
	
	public static String standardTableName(String tableSeriesName, FL_DateInterval interval) {
		if (interval != null) {
			return tableSeriesName + getIntervalLevel(interval);
		}
		
		return tableSeriesName;
	}

	
	
	
	public static String getIntervalLevel(FL_DateInterval interval) {
		switch (interval) {
		case SECONDS:
			return "By The Second";
		case HOURS:
			return "Hourly";
		case DAYS:
			return "Daily";
		case WEEKS:
			return "Weekly";
		case MONTHS:
			return "Monthly";
		case QUARTERS:
			return "Quarterly";
		case YEARS:
			return "Yearly";
		}
		return "Yearly";
	}
	
	


	public static String createNodeIdListFromCollection(
		List<String> nodeIDs
	) {
		return createNodeIdListFromCollection(
			nodeIDs, 
			null, 
			""
		);
	}
	
	
	
	
	public static String createNodeIdListFromCollection(
		List<String> nodeIDs,
		DataNamespaceHandler nameSpaceHandler, 
		String namespace
	) {
		
		if (nodeIDs == null || nodeIDs.isEmpty()) return null;
	
		StringBuilder resultString = new StringBuilder();
		
		for (String id : nodeIDs) {
			if(nameSpaceHandler == null) {
				resultString.append("'" + id + "', ");
			} else {
                if (nameSpaceHandler.getIdType(id, namespace) == ID_TYPE.HEX) {
                    resultString.append(nameSpaceHandler.toSQLId(id, namespace) + ", ");
                } else {
                    resultString.append("'" + id + "', ");
                }
			}
		}
		
		resultString.replace(
			resultString.lastIndexOf(","),
			resultString.length() - 1,
			""
		);
		return resultString.toString();
	}

	
	
	
	public static String createInClause(Collection<String> inItems) {
		return createInClause(inItems, null, null);
	}
	
	
	
	
	public static String createInClause(
		Collection<String> inItemIds, 
		DataNamespaceHandler nameSpaceHandler,
		String namespace
	) {
		
		StringBuilder resultString = new StringBuilder();
		resultString.append("(");
		
		for (String item : inItemIds) {
			if(nameSpaceHandler == null) {
				resultString.append("'" + item + "',");
			} else {
				
				if (nameSpaceHandler.getIdType(item, namespace) == ID_TYPE.HEX) {
					resultString.append(nameSpaceHandler.toSQLId(item, namespace) + ",");
				} else {
					resultString.append("'" + item + "',");
				}
			}
		}
		if (!inItemIds.isEmpty()) {
			resultString.replace(
				resultString.lastIndexOf(","),
				resultString.length(),
				")"
			);
		}
		else {
			resultString.append(")");
		}
		return resultString.toString();
	}
	
	
	
	
	public static String linkEntityTypeClause(
		FL_DirectionFilter direction, 
		FL_LinkEntityTypeFilter entityType
	) {
		
		if (entityType == FL_LinkEntityTypeFilter.ANY) return " 1=1 ";
		
		StringBuilder clause = new StringBuilder();
		String type = "A";
		if (entityType == FL_LinkEntityTypeFilter.ACCOUNT_OWNER) {
			type = "O";
		} else if (entityType == FL_LinkEntityTypeFilter.CLUSTER_SUMMARY) {
			type = "S";
		}
		
		// reverse direction - we are filtering on other entity
		switch (direction) {
		case DESTINATION:
			clause.append(" FromEntityType = '" + type + "' ");
			break;
		case SOURCE:
			clause.append(" ToEntityType = '" + type + "' ");
			break;
		case BOTH:
			clause.append(" ToEntityType = '" + type + "' AND FromEntityType = '" + type + "' ");
			break;
		}
		
		return clause.toString();
	}
}
