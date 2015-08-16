package com.shinnosuke.crawler4j.examples.news;

public class News {

	private String title;
	private String date;
	private String content;

	public News() {
		
	}
	
	public News(String title, String date, String content) {
		this.title = title;
		this.date = date;
		this.content = content;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(title).append("|").append(date).append("|").append(content);
		return sb.toString();
	}

}
