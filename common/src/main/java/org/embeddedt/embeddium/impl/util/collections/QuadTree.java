package org.embeddedt.embeddium.impl.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author XFactHD (used under terms of LGPL-3.0 with some small modifications)
 */
public final class QuadTree<T>
{
    private static final int MAX_DEPTH = 12;

    private final Rect2i rect;
    private final List<QuadTree<T>> children;
    private final Rect2i[] childRects;

    private final List<Entry<T>> entries = new ArrayList<>();

    public QuadTree(Rect2i rect, int minSize)
    {
        this(rect, minSize, 0);
    }

    private QuadTree(Rect2i rect, int minSize, int depth)
    {
        this.rect = rect;
        depth++;

        if (depth < MAX_DEPTH && rect.width() > minSize && rect.width() % 2 == 0)
        {
            children = new ArrayList<>(4);
            childRects = new Rect2i[4];

            int childWidth = rect.width() / 2;
            int childHeight = rect.height() / 2;

            if (rect.width() == rect.height() / 2)
            {
                childRects[0] = new Rect2i(rect.x(), rect.y(), rect.width(), childHeight);
                childRects[1] = null;
                childRects[2] = null;
                childRects[3] = new Rect2i(rect.x(), rect.y() + childHeight, rect.width(), childHeight);

                children.add(new QuadTree<>(childRects[0], minSize, depth));
                children.add(null);
                children.add(null);
                children.add(new QuadTree<>(childRects[3], minSize, depth));
            }
            else if (rect.height() == rect.width() / 2)
            {
                childRects[0] = new Rect2i(rect.x(), rect.y(), childWidth, rect.height());
                childRects[1] = new Rect2i(rect.x() + childWidth, rect.y(), childWidth, rect.height());
                childRects[2] = null;
                childRects[3] = null;

                children.add(new QuadTree<>(childRects[0], minSize, depth));
                children.add(new QuadTree<>(childRects[1], minSize, depth));
                children.add(null);
                children.add(null);
            }
            else
            {
                childRects[0] = new Rect2i(rect.x(), rect.y(), childWidth, childHeight);
                childRects[1] = new Rect2i(rect.x() + childWidth, rect.y(), childWidth, childHeight);
                childRects[2] = new Rect2i(rect.x() + childWidth, rect.y() + childHeight, childWidth, childHeight);
                childRects[3] = new Rect2i(rect.x(), rect.y() + childHeight, childWidth, childHeight);

                for (int i = 0; i < 4; i++)
                {
                    children.add(new QuadTree<>(childRects[i], minSize, depth));
                }
            }
        }
        else
        {
            children = null;
            childRects = null;
        }
    }

    public void insert(T item, Function<T, Rect2i> sizeFactory)
    {
        insert(item, sizeFactory.apply(item));
    }

    private void insert(T item, Rect2i size)
    {
        if (childRects != null)
        {
            for (int i = 0; i < 4; i++)
            {
                if (childRects[i] != null && rectContains(childRects[i], size))
                {
                    children.get(i).insert(item, size);
                    break;
                }
            }
        }

        entries.add(new Entry<>(item, size));
    }

    public T find(int x, int y) {
        if (!entries.isEmpty())
        {
            for (Entry<T> e : entries)
            {
                if (e.size.contains(x, y))
                {
                    return e.item;
                }
            }
        }

        if (childRects != null)
        {
            for (int i = 0; i < 4; i++)
            {
                if (childRects[i] != null && childRects[i].contains(x, y))
                {
                    T item = children.get(i).find(x, y);
                    if (item != null)
                    {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    private static boolean rectContains(Rect2i r1, Rect2i r2)
    {
        return r1.contains(r2.x(), r2.y()) && r1.contains(r2.x() + r2.width(), r2.y() + r2.height());
    }

    public int depth()
    {
        if (children != null)
        {
            int d = 0;
            for (QuadTree<T> child : children)
            {
                if (child != null)
                {
                    d = Math.max(child.depth(), d);
                }
            }
            return d + 1;
        }
        return 1;
    }

    public Rect2i minSize()
    {
        if (children != null)
        {
            Rect2i minRect = rect;
            for (QuadTree<T> child : children)
            {
                if (child != null)
                {
                    Rect2i childRect = child.minSize();
                    if (childRect.width() < rect.width() || childRect.height() < rect.height())
                    {
                        minRect = childRect;
                    }
                }
            }
            return minRect;
        }
        return rect;
    }

    private record Entry<T>(T item, Rect2i size)
    {

    }
    
    public record Rect2i(int x, int y, int width, int height) {
        public boolean contains(int x, int y) {
            return x >= this.x && y >= this.y && x <= this.x + this.width && y <= this.y + this.height;
        }
    }
}

