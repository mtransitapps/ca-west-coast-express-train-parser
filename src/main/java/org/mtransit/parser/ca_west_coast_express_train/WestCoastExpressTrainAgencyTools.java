package org.mtransit.parser.ca_west_coast_express_train;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
// http://ns.translink.ca/gtfs/notifications.zip
// http://ns.translink.ca/gtfs/google_transit.zip
// http://gtfs.translink.ca/static/latest
public class WestCoastExpressTrainAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-west-coast-express-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WestCoastExpressTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating West Coast Express train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating West Coast Express train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!RSN_WCE.contains(gRoute.getRouteShortName())) {
			return true; // exclude
		}
		return false; // keep
	}

	private static final String TRAINBUS_THS_LC = "trainbus";

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH).contains(TRAINBUS_THS_LC)) {
			return true; // TrainBus is a bus, not a train
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
		}
		if (RSN_WCE.contains(gRoute.getRouteShortName())) {
			return RID_WCE;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final List<String> RSN_WCE = Arrays.asList(new String[] { "997", "WCE", "WEST COAST EXPRESS" });
	private static final String WCE_SHORT_NAME = "WCE";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (RSN_WCE.contains(gRoute.getRouteShortName())) {
			return WCE_SHORT_NAME;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final Pattern WCE_ = Pattern.compile("((^|\\W){1}(west coast express)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = WCE_.matcher(routeLongName).replaceAll(StringUtils.EMPTY); // removing trade-mark
		return routeLongName;
	}

	private static final String AGENCY_COLOR_VIOLET = "711E8C"; // VIOLET (from PDF map)

	private static final String AGENCY_COLOR = AGENCY_COLOR_VIOLET;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		return null; // use agency color
	}

	private static final long RID_WCE = 997l;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\n%s: Unexpected trips to merge: %s and %s!\n", mTrip.getRouteId(), mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern WCE_TRAIN_TO = Pattern.compile("(^west coast express train to)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = WCE_TRAIN_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_BOUND = Pattern.compile("((east|west|north|south)(bound)?[\\s]*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION_STN = Pattern.compile("(station|stn)", Pattern.CASE_INSENSITIVE);

	private static final Pattern UNLOADING = Pattern.compile("(unload(ing)?( only)?$)", Pattern.CASE_INSENSITIVE);

	private static final String WCE_REPLACEMENT = "$2" + WCE_SHORT_NAME + "$4";

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = STATION_STN.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = UNLOADING.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = WCE_.matcher(gStopName).replaceAll(WCE_REPLACEMENT);
		gStopName = ENDS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (!StringUtils.isEmpty(gStop.getStopCode()) && Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // using stop code as stop ID
		}
		return 1000000 + Integer.parseInt(gStop.getStopId());
	}
}