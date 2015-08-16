package com.shinnosuke.crawler4j.examples.news;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class NewsCrawler extends WebCrawler {

	private static final Pattern IMAGE_EXTENSIONS = Pattern
			.compile(".*\\.(bmp|gif|jpg|png)$");
	
	private static final String filePath = "D:\\result.txt";

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {

		if (IMAGE_EXTENSIONS.matcher(url.getURL().toLowerCase()).matches()) {
			return false;
		}
		return true;
	}

	@Override
	public void visit(Page page) {
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData)page.getParseData();
			String html = htmlParseData.getHtml();
			Document doc = Jsoup.parse(html);
			Element contentElement = getCountElement(doc);
			String title = null;
			String content = null;
			if (contentElement != null) {
				title = getTitle(contentElement, doc);
				content = contentElement.text();
			}
			FileUtil.write(filePath, new News(title, null, content));
		} else {
			logger.info("page is not a html");
		}
	}

	protected Element getCountElement(Document doc) {
		pagePretreatment(doc);
		Map<Element, CountInfo> infoMap = Maps.newHashMap();
		computeInfo(doc.body(), infoMap);
		float maxScore = 0f;
		Element content = null;
		for (Map.Entry<Element, CountInfo> entry : infoMap.entrySet()) {
			float score = computeScore(entry.getKey(), infoMap);
			if (score > maxScore) {
				maxScore = score;
				content = entry.getKey();
			}
		}
		if (content == null) {
			throw new RuntimeException("content extraction failed");
		}
		return content;
	}

	class CountInfo {
		int textCount = 0;
		int linkTextCount = 0;
		int tagCount = 0;
		int linkTagCount = 0;
		float density = 0f;
		float densitySum = 0f;
		float score = 0f;
		int pTagCount = 0;
		List<Integer> leafList = Lists.newArrayList();
	}

	protected void pagePretreatment(Document doc) {
		doc.select("script,noscript,style,iframe,br").remove();
	}

	protected CountInfo computeInfo(Node node, Map<Element, CountInfo> infoMap) {

		if (node instanceof Element) {
			Element nodes = (Element) node;
			CountInfo countInfo = new CountInfo();
			for (Node childNode : nodes.childNodes()) {
				CountInfo childCountInfo = computeInfo(childNode, infoMap);
				countInfo.textCount += childCountInfo.textCount;
				countInfo.linkTextCount += childCountInfo.linkTextCount;
				countInfo.tagCount += childCountInfo.tagCount;
				countInfo.linkTagCount += childCountInfo.linkTagCount;
				countInfo.densitySum += childCountInfo.density;
				countInfo.pTagCount += childCountInfo.pTagCount;
				countInfo.leafList.addAll(childCountInfo.leafList);
			}
			countInfo.tagCount++;
			String tagName = nodes.tagName();
			if (tagName.equals("a")) {
				countInfo.linkTextCount = countInfo.textCount;
				countInfo.linkTagCount++;
			} else if (tagName.equals("p")) {
				countInfo.pTagCount++;
			}
			int pureLen = countInfo.textCount - countInfo.tagCount;
			int tagCount = countInfo.tagCount - countInfo.linkTagCount;
			if (pureLen == 0 || tagCount == 0) {
				countInfo.density = 0;
			} else {
				countInfo.density = (pureLen + 0f) / tagCount;
			}
			infoMap.put(nodes, countInfo);
			return countInfo;
		} else if (node instanceof TextNode) {
			TextNode tn = (TextNode) node;
			CountInfo countInfo = new CountInfo();
			countInfo.textCount = tn.text().length();
			countInfo.leafList.add(tn.text().length());
			return countInfo;
		}
		return new CountInfo();
	}

	protected float computeScore(Element tag, Map<Element, CountInfo> infoMap) {
		CountInfo countInfo = infoMap.get(tag);
		float var = (float) Math.sqrt(computeVariance(countInfo.leafList) + 1);
		float score = (float)(Math.log(var) * countInfo.densitySum
				* Math.log(countInfo.textCount - countInfo.linkTextCount + 1)
				* Math.log10(countInfo.pTagCount + 2));
		return score;
	}

	protected float computeVariance(List<Integer> list) {
		if (list.isEmpty()) {
			return 0;
		}
		if (list.size() == 1) {
			return list.get(0) / 2;
		}
		int sum = 0;
		for (int i : list) {
			sum += i;
		}
		float avg = sum / list.size();
		sum = 0;
		for (int i : list) {
			sum += (i - avg) * (i - avg);
		}
		sum /= list.size();
		return sum;
	}
	
	protected String getTitle(final Element contentElement, Document doc) {
		final List<Element> titleList = Lists.newArrayList();
		final List<Double> titleSim = Lists.newArrayList();
		final AtomicInteger contentIndex = new AtomicInteger();
		final String metaTitle = doc.title().trim();
		if (!metaTitle.isEmpty()) {
			doc.body().traverse(new NodeVisitor() {
				@Override
				public void head(Node node, int i) {
					if (node instanceof Element) {
						Element tag = (Element) node;
						if (tag == contentElement) {
							contentIndex.set(titleList.size());
							return;
						}
						String tagName = tag.tagName();
						if (Pattern.matches("h[1-6]", tagName)) {
							String title = tag.text().trim();
							double sim = strSim(title, metaTitle);
							titleSim.add(sim);
							titleList.add(tag);
						}
					}
				}

				@Override
				public void tail(Node node, int i) {
				}
			});
			int index = contentIndex.get();
			if (index > 0) {
				double maxScore = 0;
				int maxIndex = -1;
				for (int i = 0; i < index; i++) {
					double score = (i + 1) * titleSim.get(i);
					if (score > maxScore) {
						maxScore = score;
						maxIndex = i;
					}
				}
				if (maxIndex != -1) {
					return titleList.get(maxIndex).text();
				}
			}
		}

		Elements titles = doc.body().select(
				"*[id^=title],*[id$=title],*[class^=title],*[class$=title]");
		if (titles.size() > 0) {
			String title = titles.first().text();
			if (title.length() > 5 && title.length() < 40) {
				return titles.first().text();
			}
		}
		return null;
	}
	
	protected float strSim(String x, String y) {
		if (StringUtils.isBlank(x) || StringUtils.isBlank(y)) {
			return 0f;
		}
		float ratio = 0;
		int lenx = x.length();
		int leny = y.length();
		if (lenx > leny) {
			ratio = (lenx + 0f) / leny;
		} else {
			ratio = (leny + 0f) / lenx;
		}
		if (ratio > 2) {
			return 0f;
		}
		return (lcs(x, y) + 0f) / Math.max(lenx, leny);
	}
	
	protected int lcs(String x, String y) {
		int M = x.length();
		int N = y.length();
		if (M == 0 || N == 0) {
			return 0;
		}
		int[][] opt = new int[M + 1][N + 1];
		for (int i = M - 1; i >= 0; --i) {
			for (int j = N - 1; j >= 0; --j) {
				if (x.charAt(i) == y.charAt(j)) {
					opt[i][j] = opt[i + 1][j + 1] + 1;
				} else {
					opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
				}
			}
		}
		return opt[0][0];
	}
}
