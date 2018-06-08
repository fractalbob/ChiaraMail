package com.chiaramail.chiaramailforandroid.helper;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.XMLReader;

import android.text.Editable;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;
//import android.text.style.ImageSpan;

public class VideoTagHandler implements TagHandler {

    private List<Object> _format_stack = new LinkedList<Object>();

    public void handleTag(boolean opening, String tag, Editable output,
            XMLReader xmlReader) {
        if(tag.equalsIgnoreCase("video")) {
            processVideo(opening, output);
        } 
    }
    private void processVideo(boolean open_tag, Editable output) {
        final int length = output.length();
        if (open_tag) {
//            final Object format = new ImageSpan(ImageSpan.ALIGN_BASELINE);
//            _format_stack.add(format);
//            output.setSpan(format, length, length, Spanned.SPAN_MARK_MARK);
        } else {
            applySpan(output, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void applySpan(Editable output, int length, int flags) {
        if (_format_stack.isEmpty()) return;

        final Object format = _format_stack.remove(0);
        final Object span = getLast(output, format.getClass());
        final int where = output.getSpanStart(span);

        output.removeSpan(span);

        if (where != length)
            output.setSpan(format, where, length, flags);
    }
    private Object getLast(Editable text, Class kind) {
        @SuppressWarnings("unchecked")
        final Object[] spans = text.getSpans(0, text.length(), kind);

        if (spans.length != 0)
            for (int i = spans.length; i > 0; i--)
                if (text.getSpanFlags(spans[i-1]) == Spannable.SPAN_MARK_MARK)
                    return spans[i-1];

        return null;
    }
}
