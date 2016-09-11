package de.lippertmarkus.rapla2csv;

import java.time.LocalDate;
import java.time.LocalTime;

public class Lesson
{
	private String title;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private String professor;
	private String room;

	public Lesson(String title, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String professor, String room)
	{
		this.title = title;
		this.startDate = startDate;
		this.endDate = endDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.professor = professor;
		this.room = room;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public LocalDate getStartDate()
	{
		return startDate;
	}

	public void setStartDate(LocalDate startDate)
	{
		this.startDate = startDate;
	}

	public LocalDate getEndDate()
	{
		return endDate;
	}

	public void setEndDate(LocalDate endDate)
	{
		this.endDate = endDate;
	}

	public LocalTime getStartTime()
	{
		return startTime;
	}

	public void setStartTime(LocalTime startTime)
	{
		this.startTime = startTime;
	}

	public LocalTime getEndTime()
	{
		return endTime;
	}

	public void setEndTime(LocalTime endTime)
	{
		this.endTime = endTime;
	}

	public String getProfessor()
	{
		return professor;
	}

	public void setProfessor(String professor)
	{
		this.professor = professor;
	}

	public String getRoom()
	{
		return room;
	}

	public void setRoom(String room)
	{
		this.room = room;
	}
}
