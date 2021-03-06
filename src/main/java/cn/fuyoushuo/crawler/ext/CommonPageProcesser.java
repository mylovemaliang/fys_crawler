package cn.fuyoushuo.crawler.ext;

import cn.fuyoushuo.crawler.config.CrawlerConfig;
import cn.fuyoushuo.crawler.config.FieldItem;
import cn.fuyoushuo.crawler.config.NestedItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.List;

/**
 * Created by MALIANG on 2016/4/29.
 */
public class CommonPageProcesser implements PageProcessor {

    private CrawlerConfig crawlerConfig;

    public CommonPageProcesser(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
    }

    Site site = Site.me().setSleepTime(1000);

    @Override
    public void process(Page page) {
        if(!crawlerConfig.hasChildTask()){
           List<FieldItem> fields = crawlerConfig.getFields();
           if(CollectionUtils.isEmpty(fields)) return;
           for(FieldItem item : fields){
                 String itemName = item.getName();
                 String items = item.getItems();
                 if(!StringUtils.isEmpty(items) && items.contains(",")){
                      handlerObjectField(page,item);
                 }
                 else if(!StringUtils.isEmpty(item.getType())&& "nested".equals(item.getType())){
                      handlerNestedField(page,item);
                 }
                 else{
                      handlerCommonField(page,item);
                 }
               }
           }
       }



    @Override
    public Site getSite() {
        return site;
    }

    /**
     * 处理对象结构数据(非nested)
     * @param page
     * @param item
     */
    private void handlerObjectField(Page page, FieldItem item) {
        Html html = page.getHtml();
        String name = item.getName();
        String items = item.getItems();
        String[] subNames = items.split(",");
        int size = subNames.length;
        Selectable selectable = item.parseRegex(html);
        if(!selectable.match()); page.putField(name,null);
        List<String> allList = selectable.all();
        if(CollectionUtils.isEmpty(allList));page.putField(name,null);
        JSONArray jsonArray = new JSONArray();
        int total = allList.size();
        int lay = total/size;
        for(int i=0;i<lay;i++) {
            JSONObject object = new JSONObject();
            for(int j=0;j<size;j++){
                object.put(subNames[j],allList.get(i*size+j));
            }
            jsonArray.add(object);
        }
        page.putField(name,jsonArray);
    }

    /**
     * 处理普通结构数据
     * @param page
     * @param item
     */
    private void handlerCommonField(Page page,FieldItem item) {
        Html html = page.getHtml();
        String name = item.getName();
        String items = item.getItems();
        //普通字段，对象字段本就不存在
        if(!StringUtils.isEmpty(items)) return;
        Selectable selectable = item.parseRegex(html);
        if(!selectable.match()){
            page.putField(name,null);
            return;
        }
        String value = selectable.get();
        page.putField(name,value);
    }

    /**
     * 处理对象结构数据(nested)
     * @param page
     * @param item
     */
    private void handlerNestedField(Page page, FieldItem item) {
        List<NestedItem> nestedItems = item.getNestedItems();
        if(CollectionUtils.isEmpty(nestedItems)){
            return;
        }
        Html html = page.getHtml();
        String name = item.getName();
        Selectable selectableMain = item.parseRegex(html);

        if(!selectableMain.match()){
            return;
        }
        List<Selectable> nodes = selectableMain.nodes();
        if(CollectionUtils.isEmpty(nodes)){
            return;
        }
        int count = 0;
        Integer size = item.getSize();
        JSONArray jsonArray = new JSONArray();
        for(Selectable node : nodes){
          if(count == size){
              break;
          }
          JSONObject jsonObject = new JSONObject();
          for(NestedItem currItem : nestedItems){
              String currItemName = currItem.getName();
              Selectable selectable = currItem.parseRegex(node);
              if(!selectable.match()) continue;
              String result = selectable.get();
              jsonObject.put(currItemName,result);
          }
          jsonArray.add(jsonObject);
          count++;
    }
    page.putField(name,jsonArray);
}

}
