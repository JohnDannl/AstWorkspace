package com.closeli.draggableviewpager;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.arcsoft.closeli.draggableviewpager.DraggableViewPager;
import com.arcsoft.closeli.draggableviewpager.DraggableViewPagerAdapter;
import com.closeli.draggableviewpager.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExampleDraggableViewPagerAdapter implements DraggableViewPagerAdapter {

    List<Page> pages = new ArrayList<Page>();
    private Context context;
    private DraggableViewPager gridview;

    public ExampleDraggableViewPagerAdapter(Context context, DraggableViewPager gridview) {
        super();
        this.context = context;
        this.gridview = gridview;

        Page page1 = new Page();
        int totalCount=0;
        List<Item> items = new ArrayList<Item>();
        for(totalCount=1;totalCount<=4;totalCount++){
            items.add(new Item(totalCount, "Item "+totalCount, R.drawable.ic_launcher));
        }
        page1.setItems(items);
        pages.add(page1);

        Page page2 = new Page();
        items = new ArrayList<Item>();
        for(;totalCount<=8;totalCount++){
            items.add(new Item(totalCount, "Item "+totalCount, R.drawable.ic_launcher));
        }
        page2.setItems(items);
        pages.add(page2);

        Page page3 = new Page();
        items = new ArrayList<Item>();
        for(;totalCount<=11;totalCount++){
            items.add(new Item(totalCount, "Item "+totalCount, R.drawable.ic_launcher));
        }
        page3.setItems(items);
        pages.add(page3);

        Page page4 = new Page();
        items = new ArrayList<Item>();
        for(;totalCount<=13;totalCount++){
            items.add(new Item(totalCount, "Item "+totalCount, R.drawable.ic_launcher));
        }
        page4.setItems(items);
        pages.add(page4);
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
        final LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = layoutInflater.inflate(R.layout.my_text_view, null);
        final TextView textView = (TextView) view.findViewById(R.id.info_text);
        Item item = getItem(page, index);
        textView.setText(item.getName());
        textView.setBackgroundDrawable(context.getResources().getDrawable(item.getDrawable()));
        return view;
    }

    private Item getItem(int page, int index) {
        List<Item> items = itemsInPage(page);
        return items.get(index);
    }

    @Override
    public int rowCount() {
        return -1;
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

            for (Item item : page.getItems()) {
                Log.d("Item", Long.toString(item.getId()));
            }
        }
    }

    private Page getPage(int pageIndex) {
        return pages.get(pageIndex);
    }

    @Override
    public void swapItems(int pageIndex, int itemIndexA, int itemIndexB) {
        getPage(pageIndex).swapItems(itemIndexA, itemIndexB);
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
            startpage.addItem(0,landingPageLastItem);
        }
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
            startpage.addItem(landingPageFirstItem);
        }
    }

    @Override
    public void deleteItem(int pageIndex, int itemIndex) {
        getPage(pageIndex).deleteItem(itemIndex);
    }


    @Override
    public int getPageWidth(int page) {
        return 0;
    }

    @Override
    public Object getItemAt(int page, int index) {
        return getPage(page).getItems().get(index);
    }

    @Override
    public boolean disableZoomAnimationsOnChangePage() {
        return true;
    }
}
