package pageProcessor;

import java.util.List;

import javax.management.JMException;

import org.apache.log4j.BasicConfigurator;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

public class BaiduMusicPageProcessor implements PageProcessor {

	private Site baidu = Site.me().setRetryTimes(3).setSleepTime(1000);

	@Override
	public void process(Page page) {
		// TODO Auto-generated method stub
		String id = "";
		// 保存当前页面需要的信息
		if (page.getUrl().regex("http://music.baidu.com/artist/\\d*").match()) {
			id = page.getUrl().regex("http://music.baidu.com/artist/(\\d+)").get();
			page.putField("sing_uid", id);
			getValue(page, page.getHtml());
			page.addTargetRequest("");
			page.addTargetRequest("http://music.baidu.com/data/user/getsongs?start=25&ting_uid=" + id + "&order=hot");
		} else {
			id = page.getUrl().regex(".*?ting_uid=(\\d+).*").get();
			page.putField("sing_uid", id);
			String start = page.getUrl().regex(".*?start=(\\d+).*").get();
			int index = Integer.parseInt(start) + 25;
			Selectable content = page.getJson().jsonPath("$.data.html");
			if (parseJson(page, content) >= 25) {
				page.addTargetRequest(
						"http://music.baidu.com/data/user/getsongs?start=" + index + "&ting_uid=" + id + "&order=hot");
			}
		}
		// 从当前页面寻找下一个目标URL
		if (!page.getUrl().regex("http://music.baidu.com/artist/\\d+").match())
			page.addTargetRequests(page.getHtml().links().regex("http://music.baidu.com/artist/\\d+").all());
	}

	public void getValue(Page page, Selectable content) {
		page.putField("singer", content.xpath("//*[@id='baseInfo']/div[2]/div/h2/text()"));
		page.putField("songIdsList", content.links().regex("song/(\\d*)").all());
		page.putField("songNameList", content.xpath("//div[@class=song-list-wrap]").$("span.song-title>a:first-child")
				.xpath("//a/text()").all());
	}

	public int parseJson(Page page, Selectable content) {
		List<String> datas = content.regex("song\\/(\\d*)").all();
		page.putField("songIdsList", datas);
		page.putField("songNameList", content.regex("data-film=\\\"null\\\">(.*?)<").all());
		return datas.size();
	}

	@Override
	public Site getSite() {
		// TODO Auto-generated method stub
		return baidu;
	}

	public static void main(String[] args) throws JMException {
		BasicConfigurator.configure();
		Spider baiduSpider = Spider.create(new BaiduMusicPageProcessor())
				.addUrl("http://music.baidu.com/artist/")
				.addPipeline(new FilePipeline("d:\\work\\Spider"))
				.thread(30);
		SpiderMonitor.instance().register(baiduSpider);
		baiduSpider.start();
	}
}
