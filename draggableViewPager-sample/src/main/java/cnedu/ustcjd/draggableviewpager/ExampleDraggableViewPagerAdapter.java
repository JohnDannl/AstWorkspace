package cnedu.ustcjd.draggableviewpager;

import android.content.Context;
import android.util.Log;
import android.view.View;

import cnedu.ustcjd.widget.draggableviewpager.DraggableViewPager;
import cnedu.ustcjd.widget.draggableviewpager.DraggableViewPagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExampleDraggableViewPagerAdapter implements DraggableViewPagerAdapter {

    /**
     * 每行item个数
     */
    private static final int ROW_SIZE = 2;
    /**
     * 每列item个数
     */
    private static final int COLUMN_SIZE = 2;
    private static final int PAGE_ITEM_SIZE = 4;

    List<Page> pages = new ArrayList<Page>();
    private Context context;
    private DraggableViewPager gridview;

    public ExampleDraggableViewPagerAdapter(Context context, DraggableViewPager gridview) {
        super();
        this.context = context;
        this.gridview = gridview;

        int totalCount = 0;
        for (int i = 0; i < 4; i++) {
            Page page = new Page();
            List<Item> items = new ArrayList<Item>();
            for (int j = 0; j < PAGE_ITEM_SIZE; j++){
                totalCount +=1;
                items.add(new Item(totalCount, "Item"+totalCount, R.drawable.ic_launcher));
            }
            page.setItems(items);
            pages.add(page);
        }
        /*Page page = new Page();
        List<Item> items = new ArrayList<Item>();
        for (int j = 0; j < PAGE_ITEM_SIZE - 1; j++){
            totalCount +=1;
            items.add(new Item(totalCount, "Item"+totalCount, R.drawable.ic_launcher));
        }
        page.setItems(items);
        pages.add(page);*/
    }

    @Override
    public int pageCount() {
        return pages.size();
    }

    private List<Item> itemsInPage(int page) {
        if (pages.size() > page) {
            return pages.get(page).getItems();
        }
        return Collections.emptyList();
    }

    @Override
    public View view(int page, int index) {
        /*final LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = layoutInflater.inflate(R.layout.my_text_view, null);
        final TextView textView = (TextView) view.findViewById(R.id.info_text);
        Item item = getItem(page, index);
        textView.setText(item.getName());
        //textView.setBackgroundDrawable(context.getResources().getDrawable(item.getDrawable()));*/
        Item item = getItem(page, index);
        final View view = new GridItemView(context, item.getName()).getView();
        return view;
    }

    private Item getItem(int page, int index) {
        List<Item> items = itemsInPage(page);
        return items.get(index);
    }

    @Override
    public int rowCount() {
        return 2;
    }

    @Override
    public int columnCount() {
        return 2;
    }

    @Override
    public int itemCountInPage(int page) {
        return itemsInPage(page).size();
    }

    public void printLayout() {
        int i = 0;
        for (Page page : pages) {
            Log.d("Page", Integer.toString(i++));

            String msg = "";
            for (Item item : page.getItems()) {
                msg += Long.toString(item.getId()) + ",";
            }
            Log.d("Item", msg);
        }
    }

    private Page getPage(int pageIndex) {
        return pages.get(pageIndex);
    }

    @Override
    public void swapItems(int pageIndex, int itemIndexA, int itemIndexB) {
        getPage(pageIndex).swapItems(itemIndexA, itemIndexB);
        printLayout();
    }

    @Override
    public void moveItemToPreviousPage(int pageIndex, int itemIndex) {
        int leftPageIndex = pageIndex - 1;
        if (leftPageIndex >= 0) {
            Page startpage = getPage(pageIndex);
            Page landingPage = getPage(leftPageIndex);

            Item landingPageLastItem=landingPage.removeItem(landingPage.size()-1);
            Item item = startpage.removeItem(itemIndex);
            landingPage.addItem(item);
            startpage.addItem(itemIndex,landingPageLastItem);
        }
        printLayout();
    }

    @Override
    public void moveItemToNextPage(int pageIndex, int itemIndex) {
        int rightPageIndex = pageIndex + 1;
        if (rightPageIndex < pageCount()) {
            Page startpage = getPage(pageIndex);
            Page landingPage = getPage(rightPageIndex);

            Item landingPageFirstItem=landingPage.removeItem(0);
            Item item = startpage.removeItem(itemIndex);
            landingPage.addItem(0,item);
            startpage.addItem(itemIndex,landingPageFirstItem);
        }
        printLayout();
    }

    @Override
    public void deleteItem(int pageIndex, int itemIndex) {
        getPage(pageIndex).deleteItem(itemIndex);
    }


    @Override
    public int getPageWidth() {
        return 0;
    }

    @Override
    public Object getItemAt(int page, int index) {
        return getPage(page).getItems().get(index);
    }

    @Override
    public boolean disableZoomAnimationsOnChangePage() {
        return false;
    }

    @Override
    public void destroyPage(int page) {
        //android.util.Log.d("XXXX","destroy page: "+page);
        }

    @Override
    public int getColumnSpacing() {
        return 7;
    }

    @Override
    public int getRowSpacing() {
        return 7;
    }

    @Override
    public int getPageSpacing() {
        return 7;
    }
}
