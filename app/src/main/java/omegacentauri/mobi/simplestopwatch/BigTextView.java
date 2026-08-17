package omegacentauri.mobi.simplestopwatch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BigTextView extends View {
    RectF bounds = new RectF();
    float lineSpacing = 1.05f;
    float letterSpacing = 1f;
    MiniFont miniFont;
    Paint paint;
    Paint basePaint;
    String text;
    String[] lines;
    float yOffsets[];
    float xOffsets[];
    float scale = 0.98f;
    float secondsScale = 1f;
    boolean hasSeconds = false;
    float secondsAlign = 0f; // 0 = left/top, 0.5 = centered, 1 = right/bottom
    boolean showSecondsColon = true;
    private RectF tmpBounds = new RectF();
    GetCenter getCenterX = null;
    GetCenter getCenterY = null;
    static final float BASE_FONT_SIZE = 50f;
    private float maxAspect = 1f;
    private double dimFraction;
    private Paint dimPaint;
    private Paint currentPaint;

    public BigTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        miniFont = new SansDigitsColon();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);

        dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setColor(Color.BLACK);

        dimFraction = 1.0;
        currentPaint = paint;

        basePaint = new Paint();
        basePaint.setTextSize(BASE_FONT_SIZE);

        setText("", true, false);
    }

    public void setDimFraction(double f) {
        dimFraction = f;
        dimPaint.setColor(dim(paint.getColor()));
        invalidate();
    }

    public void setMaxAspect(float maxAspect) {
        if (this.maxAspect != maxAspect) {
            this.maxAspect = maxAspect;
            invalidate();
        }
    }

    public void setFont(MiniFont mf) {
        this.miniFont = mf;
        invalidate();
    }

    public void setScale(float scale) {
        this.scale = scale;
        invalidate();
    }

    public void setSecondsScale(float secondsScale) {
        if (this.secondsScale != secondsScale) {
            this.secondsScale = secondsScale;
            invalidate();
        }
    }

    public void setHasSeconds(boolean hasSeconds) {
        if (this.hasSeconds != hasSeconds) {
            this.hasSeconds = hasSeconds;
            invalidate();
        }
    }

    public void setSecondsAlign(float secondsAlign) {
        if (this.secondsAlign != secondsAlign) {
            this.secondsAlign = secondsAlign;
            invalidate();
        }
    }

    public void setShowSecondsColon(boolean showSecondsColon) {
        if (this.showSecondsColon != showSecondsColon) {
            this.showSecondsColon = showSecondsColon;
            invalidate();
        }
    }

    public void setLineSpacing(float l) {
        lineSpacing = l;
        invalidate();
    }

    public void setLetterSpacing(float ls) {
        letterSpacing = ls;
        invalidate();
    }

/*    public void setText(String s, boolean b, boolean b1) {
        setText(s, false);
    } */

    public void setText(String s, Boolean force, Boolean paused) {
        Paint newPaint = paused ? dimPaint : paint;

        if (!force && text != null && text.equals(s) && currentPaint == newPaint)
            return;

        currentPaint = newPaint;

        text = new String(s);

        lines = text.split("\\n");

        int n = lines.length;
        xOffsets = new float[n];
        yOffsets = new float[n];

        invalidate();
    }

    /*
    public void measureText(RectF bounds) {
        RectF lineBounds = new RectF();
        bounds.set(0,0,0,0);

        int n = tweakedLines.length;
        for (int i = 0 ; i < n ; i++) {
            String line = tweakedLines[i];
            miniFont.getTextBounds(paint, line, 0, line.length(), lineBounds);
            yOffsets[i] = bounds.bottom - lineBounds.top;
            bounds.bottom += (int)(lineBounds.height() * (i==n-1 ? 1 : lineSpacing));
            bounds.right = Math.max(bounds.right, lineBounds.width());
        }
    }
    */

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int n = lines.length;
        if (n==0)
            return;

        float height = 0;
        float cWidth = this.getWidth(); // canvas.getWidth();
        float cHeight = this.getHeight(); // canvas.getHeight();
        float cx = cWidth / 2f;
        float maxWidth = 0;

        boolean reclaimLastLine = hasSeconds && secondsScale < 1f;

        for (int i = 0 ; i < n ; i++) {
            String line = lines[i];
            if (reclaimLastLine && i == n - 1)
                computeShrunkLastLineBounds(line, bounds);
            else
                miniFont.getTextBounds(basePaint, letterSpacing, line, 0, line.length(), bounds);
            yOffsets[i] = height - bounds.top;
            height += bounds.height() * (i==n-1 ? 1 : lineSpacing);
            xOffsets[i] = -bounds.centerX();
            maxWidth = Math.max(maxWidth, bounds.width());
        }

        if (maxWidth == 0 || height == 0)
            return;

        float adjustX;
        float adjustY;

        float adjust = scale * Math.max(cWidth / maxWidth, cHeight / height);
        if (adjust * maxWidth > cWidth * scale)
            adjustX = scale * cWidth / maxWidth;
        else
            adjustX = adjust;
        if (adjust * height > cHeight * scale)
            adjustY = scale * cHeight / height;
        else
            adjustY = adjust;

        if (adjustX > maxAspect * adjustY)
            adjustX = maxAspect * adjustY;
        else if (adjustY > maxAspect * adjustX)
            adjustY = maxAspect * adjustX;

        currentPaint.setTextSize(adjustY * BASE_FONT_SIZE);
        currentPaint.setTextScaleX(adjustX / adjustY);

        float dy = cHeight/2f - adjustY * height/2f;

        cx = adjustCenter(cWidth, getCenterX, adjustX * maxWidth);
        dy += adjustCenter(cHeight, getCenterY, adjustY * height) - cHeight/2f;

        float fullTextSize = currentPaint.getTextSize();
        boolean adjustLastLine = hasSeconds && lastLineNeedsAdjustment();

        for (int i = 0 ; i < n ; i++) {
            String line = lines[i];
            float x = cx + adjustX * xOffsets[i];
            float y = dy + adjustY * yOffsets[i];

            if (adjustLastLine && i == n - 1) {
                // last line contains (or, in a multiline layout, entirely is) the seconds field;
                // everything up to and including a trailing colon is drawn at full size, the
                // seconds themselves (plus any fractional suffix) are drawn at secondsScale,
                // and secondsAlign decides where the space freed up by shrinking them goes
                int idx = line.lastIndexOf(':');
                String beforeColon = idx >= 0 ? line.substring(0, idx) : "";
                boolean hasColon = idx >= 0;
                String secondsPart = idx >= 0 ? line.substring(idx + 1) : line;

                if (beforeColon.length() > 0) {
                    currentPaint.setTextSize(fullTextSize);
                    miniFont.drawText(canvas, beforeColon, x, y, currentPaint, letterSpacing);
                    x += miniFont.measureAdvance(currentPaint, letterSpacing, beforeColon);
                }
                if (hasColon) {
                    currentPaint.setTextSize(fullTextSize);
                    if (showSecondsColon)
                        miniFont.drawText(canvas, ":", x, y, currentPaint, letterSpacing);
                    x += miniFont.measureAdvance(currentPaint, letterSpacing, ":");
                }

                if (secondsScale < 1f) {
                    if (n == 1) {
                        // landscape: the seconds share the line with the rest of the digits.
                        // secondsAlign picks top/center/bottom, measured against the actual
                        // rendered bounds of the hour/minute text itself (not an assumed origin)
                        if (beforeColon.length() > 0) {
                            currentPaint.setTextSize(fullTextSize);
                            miniFont.getTextBounds(currentPaint, letterSpacing, beforeColon, 0, beforeColon.length(), tmpBounds);
                            float prefixTop = y + tmpBounds.top;
                            float prefixBottom = y + tmpBounds.bottom;

                            currentPaint.setTextSize(fullTextSize * secondsScale);
                            miniFont.getTextBounds(currentPaint, letterSpacing, secondsPart, 0, secondsPart.length(), tmpBounds);
                            float secTop = y + tmpBounds.top;
                            float secBottom = y + tmpBounds.bottom;

                            float target = prefixTop + secondsAlign * (prefixBottom - prefixTop);
                            float current = secTop + secondsAlign * (secBottom - secTop);
                            y += target - current;
                        }
                    }
                    else {
                        // portrait: the whole line is the seconds, on its own row. secondsAlign
                        // picks left/center/right, measured against the actual rendered bounds of
                        // the row directly above (the minutes, typically)
                        String aboveLine = lines[n - 2];
                        currentPaint.setTextSize(fullTextSize);
                        miniFont.getTextBounds(currentPaint, letterSpacing, aboveLine, 0, aboveLine.length(), tmpBounds);
                        float aboveX = cx + adjustX * xOffsets[n - 2];
                        float aboveLeft = aboveX + tmpBounds.left;
                        float aboveRight = aboveX + tmpBounds.right;

                        currentPaint.setTextSize(fullTextSize * secondsScale);
                        miniFont.getTextBounds(currentPaint, letterSpacing, secondsPart, 0, secondsPart.length(), tmpBounds);
                        float secLeft = x + tmpBounds.left;
                        float secRight = x + tmpBounds.right;

                        float target = aboveLeft + secondsAlign * (aboveRight - aboveLeft);
                        float current = secLeft + secondsAlign * (secRight - secLeft);
                        x += target - current;
                    }
                }

                currentPaint.setTextSize(fullTextSize * secondsScale);
                miniFont.drawText(canvas, secondsPart, x, y, currentPaint, letterSpacing);
                currentPaint.setTextSize(fullTextSize);
            }
            else {
                miniFont.drawText(canvas, line, x, y, currentPaint, letterSpacing);
            }
        }
    }

    // computes the ink bounds of the last line as it will actually be rendered, with the
    // seconds field (after any colon, whose width is always reserved) measured at
    // secondsScale instead of full size, so the auto-fit sizing below reclaims the space
    // a shrunk seconds field frees up instead of leaving it as a dead gap
    private void computeShrunkLastLineBounds(String line, RectF outBounds) {
        int idx = line.lastIndexOf(':');
        String prefix = idx >= 0 ? line.substring(0, idx + 1) : "";
        String secondsPart = idx >= 0 ? line.substring(idx + 1) : line;

        float savedSize = basePaint.getTextSize();

        if (prefix.length() > 0) {
            RectF prefixBounds = new RectF();
            miniFont.getTextBounds(basePaint, letterSpacing, prefix, 0, prefix.length(), prefixBounds);
            float prefixAdvance = miniFont.measureAdvance(basePaint, letterSpacing, prefix);

            basePaint.setTextSize(savedSize * secondsScale);
            miniFont.getTextBounds(basePaint, letterSpacing, secondsPart, 0, secondsPart.length(), outBounds);
            basePaint.setTextSize(savedSize);

            outBounds.left += prefixAdvance;
            outBounds.right += prefixAdvance;
            outBounds.union(prefixBounds);
        }
        else {
            basePaint.setTextSize(savedSize * secondsScale);
            miniFont.getTextBounds(basePaint, letterSpacing, secondsPart, 0, secondsPart.length(), outBounds);
            basePaint.setTextSize(savedSize);
        }
    }

    private boolean lastLineNeedsAdjustment() {
        return secondsScale < 1f || !showSecondsColon;
    }

    private float adjustCenter(float canvasSize, GetCenter c, float size) {
        float canvasCenter = canvasSize / 2f;
        if (c == null)
            return canvasCenter;
        float drawingCenter = c.getCenter();
        if (canvasCenter == drawingCenter)
            return canvasCenter;
        float half = size / 2f;
        if (drawingCenter < canvasCenter) {
            if (half <= drawingCenter)
                return drawingCenter;
            else
                return half;
        }
        else {
            if (drawingCenter + half <= canvasSize) {
                return drawingCenter;
            }
            else
                return canvasSize - half;
        }
    }

    private int dim(int color) {
        int a = (color >> 24) & 0xff;
        int r = (color >> 16) & 0xff;
        int g = (color >>  8) & 0xff;
        int b = (color      ) & 0xff;
        if (r == 0 && g == 0 && b == 0) {
            r = (int) (255 * (1-dimFraction));
            g = r;
            b = r;
        }
        else {
            StopWatch.debug("color "+r+" "+g+" "+b);
            r = (int) (r * dimFraction);
            g = (int) (g * dimFraction);
            b = (int) (b * dimFraction);
            StopWatch.debug("new color "+r+" "+g+" "+b);
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void setTextColor(int textColor) {
        if (textColor != paint.getColor()) {
            paint.setColor(textColor);
            dimPaint.setColor(dim(textColor));
            invalidate();
        }
    }

    static interface GetCenter {
        float getCenter();
    }
}
