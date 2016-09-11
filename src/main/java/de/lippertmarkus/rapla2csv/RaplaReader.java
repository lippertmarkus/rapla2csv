package de.lippertmarkus.rapla2csv;


import org.apache.commons.cli.*;
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
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaplaReader
{
	private LocalDate dateFrom;
	private LocalDate dateUntil;
	private URIBuilder raplaLink;
	private List<Lesson> allLessons = new ArrayList<>();

	public RaplaReader(LocalDate from, LocalDate until, URI raplaLink) throws IOException
	{
		this(from, until, new URIBuilder(raplaLink));
	}

	public RaplaReader(LocalDate from, LocalDate until, URL raplaLink) throws URISyntaxException, IOException
	{
		this(from, until, new URIBuilder(raplaLink.toURI()));
	}

	public RaplaReader(LocalDate from, LocalDate until, URIBuilder raplaLink) throws IOException
	{
		dateFrom = from;
		dateUntil = until;
		this.raplaLink = prepareRaplaURI(raplaLink);

		getLessonsFromRapla();
	}

	/**
	 * Checks for a calendar key in a Rapla URL and removes all other parameters.
	 * TODO support for calendars by user, without key, e.g. ?page=calendar&user=foo&file=bar
	 *
	 * @param raplaURI URL to Rapla page
	 * @return Prepared Rapla URL
	 * @throws MalformedURLException
	 */
	private URIBuilder prepareRaplaURI(URIBuilder raplaURI) throws MalformedURLException
	{
		// find rapla key in url
		Optional<NameValuePair> optionalRaplaKey = raplaURI.getQueryParams().stream().filter(p -> p.getName().equals("key")).findFirst();

		if (optionalRaplaKey.isPresent())
		{
			String raplaKey = optionalRaplaKey.get().getValue();

			// remove all unnecessary parameters from url
			raplaURI.clearParameters();
			raplaURI.addParameter("key", raplaKey);

			return raplaURI;
		}
		else throw new MalformedURLException("Rapla URL is invalid or contains no key");
	}

	private void getLessonsFromRapla() throws IOException
	{
		/*
			rapla webpage always show whole week but no dates for the lessons, so
			we have to get date of the mondays to filter the week via URL parameter
		 */
		LocalDate monday = dateFrom.with(DayOfWeek.MONDAY);
		raplaLink.addParameter("day", "");
		raplaLink.addParameter("month", "");
		raplaLink.addParameter("year", "");

		while (monday.isBefore(dateUntil) || monday.isEqual(dateUntil))
		{
			// set url parameters to show current week
			raplaLink.setParameter("day", monday.getDayOfMonth() + "");
			raplaLink.setParameter("month", monday.getMonthValue() + "");
			raplaLink.setParameter("year", monday.getYear() + "");

			// get HTML document. lessons information is inside span with CSS class .tooltip
			Document doc = Jsoup.connect(raplaLink.toString()).get();
			Elements lessons = doc.select(".tooltip");

			for (Element lesson : lessons)
			{
				Elements data = lesson.select("td"); // get all td's with lesson info inside the tooltip

				String dayTime = lesson.child(1).text(); // 2nd td element contains weekday (German) and time
				String weekDay;
				LocalTime timeFrom;
				LocalTime timeUntil;
				// extract weekday and time, e.g. "Mo 08:15-11:30 wöchentlich"
				Pattern r = Pattern.compile("([a-zA-Z]{2}).* ([0-9:]*)-([0-9:]*)");
				Matcher m = r.matcher(dayTime);
				if (m.find())
				{
					weekDay = m.group(1);
					timeFrom = LocalTime.parse(m.group(2));
					timeUntil = LocalTime.parse(m.group(3));
				}
				else continue; // if no date or time is found, skip this lesson

				// get date from german (!) weekdays
				LocalDate date = monday;
				switch (weekDay)
				{
					case "Mo":
						break;
					case "Di":
						date = date.plusDays(1);
						break;
					case "Mi":
						date = date.plusDays(2);
						break;
					case "Do":
						date = date.plusDays(3);
						break;
					case "Fr":
						date = date.plusDays(4);
						break;
					case "Sa":
						date = date.plusDays(5);
						break;
					case "So":
						date = date.plusDays(6);
						break;
					default:
						continue; // if weekday is not distinct, skip lesson
				}

				// date informations collected, check if date is within given range, else skip
				if (date.isBefore(dateFrom) || date.isAfter(dateUntil)) continue;

				String title = data.get(1).text(); // title of the lesson

				String room = "";
				r = Pattern.compile("(RB[^,]*)"); // extract room from all resources TODO: generalize
				m = r.matcher(data.get(data.size() - 3).text()); // 2nd last td-element is the room
				if (m.find()) room = m.group(1);

				String prof = "";
				r = Pattern.compile("^([^,]*), ([^,]*)"); // extract prename and surname of professor
				m = r.matcher(data.get(data.size() - 1).text()); // last td-element is professor
				if (m.find()) prof = m.group(2) + " " + m.group(1);

				allLessons.add(new Lesson(title, date, date, timeFrom, timeUntil, prof, room));
			}

			// goto next week
			monday = monday.plusWeeks(1);
		}
	}

	public void exportToCSV(String filename) throws IOException
	{
		// header for calendar CSV files
		String fileContent = "Subject,Start Date,Start Time,End Date,End Time,Description,Location\r\n";

		for (Lesson lesson : allLessons)
		{
			fileContent += "\"" + lesson.getTitle() + "\","
					+ lesson.getStartDate() + ","
					+ lesson.getStartTime() + ","
					+ lesson.getEndDate() + ","
					+ lesson.getEndTime() + ",\""
					+ lesson.getProfessor() + "\"," + "\""
					+ lesson.getRoom() + "\"\r\n";
		}

		Files.write(Paths.get(filename), fileContent.getBytes());
	}
}