package com.shinnosuke.crawler4j.examples.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class NewsCrawlerController {

	private static final Logger logger = LoggerFactory.getLogger(NewsCrawlerController.class);
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			logger.info("Need parameters: ");
			logger.info("\t rootFolder (it will contain intermediate crawl data)");
			logger.info("\t numbersOfCrawlers (number of concurrent threads)");
			return;
		}
		CrawlConfig config = getConfig(args);
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setEnabled(false);
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
		
		controller.addSeed("http://news.sina.com.cn/");
		controller.addSeed("http://news.163.com/");
		controller.addSeed("http://news.ifeng.com/");
		controller.start(NewsCrawler.class, Integer.parseInt(args[1]));
	}
	
	private static CrawlConfig getConfig(String[] args) {
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(args[0]);
		config.setPolitenessDelay(1000);
		config.setMaxDepthOfCrawling(2);
		config.setMaxPagesToFetch(1000);
		config.setIncludeBinaryContentInCrawling(false);
		config.setResumableCrawling(true);
		return config;
	}
}
