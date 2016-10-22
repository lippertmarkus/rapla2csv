package de.lippertmarkus.rapla2csv;


import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for extracting lessons/appointments from a rapla web calendar.
 */
public class RaplaReader
{
    /**
     * Begin of the extraction date range
     */
    private LocalDate dateFrom;

    /**
     * End of the extraction date range
     */
    private LocalDate dateUntil;

    /**
     * Link to the rapla web calendar view with a key or a user and file
     */
    private URIBuilder raplaLink;

    /**
     * List of the extracted lessons
     */
    private List<Lesson> extractedLessons = new ArrayList<>();

    /**
     * Counter for all processed lessons while extraction
     */
    private int countLessons = 0;

    /**
     * Counter for skipped lessons while extraction
     */
    private int countSkippedLessons = 0;


    /**
     * Constructs a new instance out of a rapla URI
     *
     * @param from      date from when to extract the lesson data
     * @param until     date until when to extract the lesson data
     * @param raplaLink rapla URI
     * @throws IOException if rapla link is no valid URI
     */
    public RaplaReader(LocalDate from, LocalDate until, URI raplaLink) throws IOException
    {
        this(from, until, new URIBuilder(raplaLink));
    }

    /**
     * Constructs a new instance out of a rapla URL
     *
     * @param from      date from when to extract the lesson data
     * @param until     date until when to extract the lesson data
     * @param raplaLink rapla URL
     * @throws URISyntaxException    if rapla link is no valid URL
     * @throws MalformedURLException if rapla link doesn't contain necessary parameters or is invalid
     */
    public RaplaReader(LocalDate from, LocalDate until, URL raplaLink) throws URISyntaxException, MalformedURLException
    {
        this(from, until, new URIBuilder(raplaLink.toURI()));
    }

    /**
     * Constructs a new instance out of a rapla link given as string
     *
     * @param from      date from when to extract the lesson data
     * @param until     date until when to extract the lesson data
     * @param raplaLink rapla link as a string
     * @throws URISyntaxException    if rapla link is not valid
     * @throws MalformedURLException if rapla link doesn't contain necessary parameters or is invalid
     */
    public RaplaReader(LocalDate from, LocalDate until, String raplaLink) throws URISyntaxException, MalformedURLException
    {
        this(from, until, new URIBuilder(raplaLink));
    }

    /**
     * Constructs a new instance out of an rapla link as URIBuilder object
     *
     * @param from      date from when to extract the lesson data
     * @param until     date until when to extract the lesson data
     * @param raplaLink rapla link as an URIBuilder object
     * @throws MalformedURLException if rapla link doesn't contain necessary parameters or is invalid
     */
    public RaplaReader(LocalDate from, LocalDate until, URIBuilder raplaLink) throws MalformedURLException
    {
        dateFrom = from;
        dateUntil = until;
        this.raplaLink = prepareRaplaUri(raplaLink);
    }

    /**
     * Looks for necessary GET-Parameters in rapla URL (key OR combination of page, user & file) and remove other
     * parameters (which can cause errors with Jsoup later). Key parameter is prefered when both is provided
     *
     * @param raplaURI rapla uri
     * @return cleaned rapla uri
     * @throws MalformedURLException if rapla uri doesn't contain necessary parameters
     */
    private URIBuilder prepareRaplaUri(URIBuilder raplaURI) throws MalformedURLException
    {
        String key = null, page = null, user = null, file = null;

        searchLoop:
        for (NameValuePair param :
                raplaURI.getQueryParams()) {

            switch (param.getName()) {
                case "key":
                    key = param.getValue();
                    break searchLoop;
                case "page":
                    page = param.getValue();
                    break;
                case "user":
                    user = param.getValue();
                    break;
                case "file":
                    file = param.getValue();
                    break;
            }
        }

        filterRaplaUri(raplaURI, key, page, user, file);

        return raplaURI;
    }

    /**
     * Cleans rapla URI to only use key or combination of page, user & file (key is preferred). All other GET parameters
     * are removed
     *
     * @param raplaURI rapla uri
     * @param key      the extracted key parameter of the url, may null
     * @param page     the extracted page parameter of the url, may null
     * @param user     the extracted user parameter of the url, may null
     * @param file     the extracted file parameter of the url, may null
     * @return cleaned rapla uri
     * @throws MalformedURLException if rapla uri is not valid
     */
    private URIBuilder filterRaplaUri(URIBuilder raplaURI, String key, String page, String user, String file) throws MalformedURLException
    {
        raplaURI.clearParameters();

        if (key != null) {
            raplaURI.addParameter("key", key);
        } else if (page != null && user != null && file != null) {
            raplaURI.addParameter("page", page);
            raplaURI.addParameter("user", user);
            raplaURI.addParameter("file", file);
        } else throw new MalformedURLException("No valid rapla url");

        return raplaURI;
    }

    /**
     * Extracts all lessons within the given time range from the rapla web calendar.
     *
     * @return list of the extracted lessons
     * @throws IOException if connection to rapla uri couldn't be established
     */
    public List<Lesson> getLessonsFromRapla() throws IOException
    {
        // rapla web page always show whole week, so we'll go along the mondays
        LocalDate currentWeekMondayDate = dateFrom.with(DayOfWeek.MONDAY);

        while (dateIsBeforeUntilDate(currentWeekMondayDate)) {
            Elements allLessonsOfWeekAsHtml = getAllLessonsOfWeekAsHtmlWithMondayDate(currentWeekMondayDate);

            for (Element lessonHtmlElement : allLessonsOfWeekAsHtml) {
                Lesson extractedLesson;

                try {
                    extractedLesson = extractLessonFromHtmlElement(currentWeekMondayDate, lessonHtmlElement);
                } catch (Exception e) {
                    System.out.println("Skipped: " + e.getMessage());
                    countSkippedLessons++;
                    continue;
                }

                // skip lessons which aren't within date range
                if (extractedLesson == null)
                    continue;

                countLessons++;
                extractedLessons.add(extractedLesson);
            }

            // goto next week
            currentWeekMondayDate = currentWeekMondayDate.plusWeeks(1);
        }

        return extractedLessons;
    }

    /**
     * Gets all HTML elements with needed information of all lessons within a week
     *
     * @param weekMondayDate the monday date of the week
     * @return all HTML elements of all lessons
     * @throws IOException if a connection to the rapla URI couldn't be established
     */
    private Elements getAllLessonsOfWeekAsHtmlWithMondayDate(LocalDate weekMondayDate) throws IOException
    {
        setRaplaUrlDateParameters(weekMondayDate, raplaLink);

        // get HTML document for current week
        Document doc = Jsoup.connect(raplaLink.toString()).get();

        // lessons information is inside span with CSS class .tooltip
        return doc.select(".tooltip");
    }

    /**
     * Checks if given date is before end of the set until date
     *
     * @param date date to check
     * @return true if date is before the until date
     */
    private boolean dateIsBeforeUntilDate(LocalDate date)
    {
        return date.isBefore(dateUntil) || date.isEqual(dateUntil);
    }

    /**
     * Constructs Lesson object out of HTML data of a lesson
     *
     * @param weekMondayDate the monday date of the week the lesson is in
     * @param lesson         the HTML data of the lesson element
     * @return a Lesson object or null if a lesson wasn't in the set date range
     * @throws Exception if a lesson was skipped because of parsing errors
     */
    private Lesson extractLessonFromHtmlElement(LocalDate weekMondayDate, Element lesson) throws Exception
    {
        // determine lesson data

        Elements data = lesson.select("td"); // get all td's with lesson info inside the tooltip

        String title = data.get(1).text(); // title of the lesson
        String room = getRoomNameFromString(data.get(data.size() - 3).text()); // 2nd last td-element is the room
        String prof = getProfessorNameFromString(data.get(data.size() - 1).text()); // last td-element is professor


        // determine date and time of the lesson

        String dayTimeString = lesson.child(1).text(); // 2nd element contains weekday (German) and time
        String weekDay;
        LocalTime timeFrom;
        LocalTime timeUntil;

        // extract weekday and time, e.g. "Mo 08:15-11:30 wöchentlich"
        Pattern r = Pattern.compile("([a-zA-Z]{2}).* ([0-9:]*)-([0-9:]*)");
        Matcher m = r.matcher(dayTimeString);
        if (m.find()) {
            weekDay = m.group(1);
            timeFrom = LocalTime.parse(m.group(2));
            timeUntil = LocalTime.parse(m.group(3));
        } else throw new Exception("Weekday and/or time of lesson '" + title + "' couldn't be determined out " +
                "of string '" + dayTimeString + "'. Week's monday date: " + weekMondayDate);

        // get date from german (!) weekdays, check if date is within given range, else skip
        LocalDate date = getDateByMondayDateAndWeekday(weekMondayDate, weekDay);
        if (!dateIsWithinRange(date))
            return null;


        return new Lesson(title, date, date, timeFrom, timeUntil, prof, room);
    }

    /**
     * Checks if a date is within the set date range
     *
     * @param date date to check
     * @return true if date is within the valid date range
     */
    private boolean dateIsWithinRange(LocalDate date)
    {
        return (date != null) && !date.isBefore(dateFrom) && !date.isAfter(dateUntil);
    }

    /**
     * Extract the room of the lesson from a string with multiple resources (persons, rooms, computers, etc.)
     *
     * @param input the resources string to extract the room from
     * @return a string with the room, if failed, an empty string
     */
    private String getRoomNameFromString(String input)
    {
        String room = "";

        Pattern pattern;
        Matcher matcher;

        // extract room from all given resources TODO: generalize
        pattern = Pattern.compile("(RB[^,]*)");
        matcher = pattern.matcher(input);

        if (matcher.find())
            room = matcher.group(1);

        System.out.println("room: " + room);

        return room;
    }

    /**
     * Extract professor name from a string, without academic titles
     *
     * @param input string to extract professor from
     * @return name of professor or empty string
     */
    private String getProfessorNameFromString(String input)
    {
        String prof = "";

        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile("^([^,]*), ([^,]*)"); // e.g. "Surname, Prename"

        matcher = pattern.matcher(input);

        if (matcher.find())
            prof = matcher.group(2) + " " + matcher.group(1); // generate "Prename Surname"

        return prof;
    }

    /**
     * Sets the URL GET-parameters to show a specific week in the rapla url calendar
     *
     * @param date      date to set the rapla URI to
     * @param raplaLink rapla URI to modify
     * @return modified rapla URI with the specified date
     */
    private URIBuilder setRaplaUrlDateParameters(LocalDate date, URIBuilder raplaLink)
    {
        // set url parameters to show current week
        raplaLink.setParameter("day", date.getDayOfMonth() + "");
        raplaLink.setParameter("month", date.getMonthValue() + "");
        raplaLink.setParameter("year", date.getYear() + "");

        return raplaLink;
    }

    /**
     * Returns the date of the given GERMAN weekday in the week starting with given monday date.
     *
     * @param mondayDate Date of the monday in the week
     * @param weekday    Weekday which date to calculate
     * @return date of the weekday in the week with the given monday date
     */
    private LocalDate getDateByMondayDateAndWeekday(LocalDate mondayDate, String weekday)
    {
        switch (weekday) {
            case "Mo":
                return mondayDate;
            case "Di":
                return mondayDate.plusDays(1);
            case "Mi":
                return mondayDate.plusDays(2);
            case "Do":
                return mondayDate.plusDays(3);
            case "Fr":
                return mondayDate.plusDays(4);
            case "Sa":
                return mondayDate.plusDays(5);
            case "So":
                return mondayDate.plusDays(6);
            default:
                return null;
        }
    }

    /**
     * Gets information about extracted/skipped lessons
     *
     * @return string with the information
     */
    public String getExtractedLessonsInfo()
    {
        return (countLessons - countSkippedLessons) + " lessons extracted, " + countSkippedLessons + " lessons skipped";
    }

    /**
     * Saves the extracted lessons to a CSV file to import into calendars like Google calendar or Outlook
     *
     * @param filename name of the CSV file to create
     * @throws Exception if there's nothing to export or if the writing of the file failed
     */
    public void exportToCSV(String filename) throws Exception
    {
        if ((countLessons - countSkippedLessons) == 0)
            throw new Exception("0 Lessons extracted, so nothing to export");

        // header for calendar CSV files
        String fileContent = "Subject,Start Date,Start Time,End Date,End Time,Description,Location\r\n";

        for (Lesson lesson : extractedLessons) {
            fileContent += "\"" + lesson.getTitle() + "\","
                    + lesson.getStartDate() + ","
                    + lesson.getStartTime() + ","
                    + lesson.getEndDate() + ","
                    + lesson.getEndTime() + ","
                    + "\"" + lesson.getProfessor() + "\","
                    + "\"" + lesson.getRoom() + "\"\r\n";
        }

        Files.write(Paths.get(filename), fileContent.getBytes());
    }
}