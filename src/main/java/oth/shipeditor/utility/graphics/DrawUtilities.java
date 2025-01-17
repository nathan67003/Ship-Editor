package oth.shipeditor.utility.graphics;

import lombok.extern.log4j.Log4j2;
import oth.shipeditor.utility.Utility;
import oth.shipeditor.utility.overseers.StaticController;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ontheheavens
 * @since 19.07.2023
 */
@SuppressWarnings("ClassWithTooManyMethods")
@Log4j2
public final class DrawUtilities {

    @SuppressWarnings("StaticCollection")
    private static final Map<Float, Stroke> CACHED_STROKES = new HashMap<>();

    private static final DrawMode DRAW_MODE = DrawMode.FAST;

    private DrawUtilities() {
    }

    public static void drawScreenLine(Graphics2D g, Point2D start, Point2D finish,
                                         Color color, float thickness) {
        Stroke originalStroke = g.getStroke();
        Color originalColor = g.getColor();

        g.setColor(color);
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) start.getX(), (int) start.getY(), (int) finish.getX(), (int) finish.getY());

        g.setStroke(originalStroke);
        g.setColor(originalColor);
    }

    private static void drawDynamicCross(Graphics2D g,
                                         AffineTransform worldToScreen,
                                         Point2D positionWorld,
                                         DrawingParameters miscParameters) {
        double worldSize = miscParameters.getWorldSize();
        double minScreenSize = miscParameters.getScreenSize();

        double worldThickness = miscParameters.getWorldThickness();

        Shape cross = ShapeUtilities.createPerpendicularThickCross(positionWorld, worldSize, worldThickness);
        Shape transformed = ShapeUtilities.ensureDynamicScaleShape(worldToScreen,
                positionWorld, cross, minScreenSize);

        Color color = miscParameters.getPaintColor();
        DrawUtilities.fillShape(g, transformed,color);
    }

    public static void outlineShape(Graphics2D g, Shape shape, Paint color, float strokeWidth) {
        Stroke cached = CACHED_STROKES.get(strokeWidth);
        if (cached == null) {
            cached = new BasicStroke(strokeWidth);
            CACHED_STROKES.put(strokeWidth, cached);
        }
        DrawUtilities.outlineShape(g, shape, color, cached);
    }

    private static void outlineShape(Graphics2D g, Shape shape, Paint color, Stroke stroke) {
        DrawUtilities.outlineShape(g, shape, color, stroke, DRAW_MODE);
    }

    @SuppressWarnings("WeakerAccess")
    public static void outlineShape(Graphics2D g, Shape shape, Paint color,
                                    Stroke stroke, DrawMode mode) {
        RenderingHints hints = g.getRenderingHints();
        switch (mode) {
            case NORMAL -> {}
            case QUALITY -> g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            case FAST -> {
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_SPEED);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                        RenderingHints.VALUE_COLOR_RENDER_SPEED);
            }
        }

        Paint old = g.getPaint();
        Stroke oldStroke = g.getStroke();
        g.setStroke(stroke);
        g.setPaint(color);

        g.draw(shape);

        g.setRenderingHints(hints);
        g.setStroke(oldStroke);
        g.setPaint(old);
    }

    @SuppressWarnings("WeakerAccess")
    public static void fillShape(Graphics2D g, Shape shape, Paint fill) {
        Paint old = g.getPaint();
        g.setPaint(fill);
        g.fill(shape);
        g.setPaint(old);
    }

    public static void drawOutlined(Graphics2D g, Shape shape, Paint color) {
        DrawUtilities.drawOutlined(g, shape, color, false);
    }

    @SuppressWarnings({"BooleanParameter", "WeakerAccess"})
    public static void drawOutlined(Graphics2D g, Shape shape, Paint color, boolean quality) {
        float widthFive = 5.0f;
        Stroke cachedFive = CACHED_STROKES.get(widthFive);
        if (cachedFive == null) {
            cachedFive = new BasicStroke(widthFive);
            CACHED_STROKES.put(widthFive, cachedFive);
        }

        float widthThree = 3.0f;
        Stroke cachedThree = CACHED_STROKES.get(widthThree);
        if (cachedThree == null) {
            cachedThree = new BasicStroke(widthThree);
            CACHED_STROKES.put(widthThree, cachedThree);
        }

        DrawUtilities.drawOutlined(g, shape, color, quality, cachedFive, cachedThree);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    public static void drawAngledCirclePointer(Graphics2D g, AffineTransform worldToScreen, Shape circle,
                                               double circleRadius, double rawAngle, Point2D position,
                                               Paint color, double paintMultiplier) {
        double transformedAngle = Utility.transformAngle(rawAngle);

        Point2D lineEndpoint = ShapeUtilities.getPointInDirection(position,
                transformedAngle, 0.5f * paintMultiplier);
        Point2D closestIntersection = ShapeUtilities.getPointInDirection(position,
                transformedAngle, circleRadius);

        Shape angleLine = new Line2D.Double(lineEndpoint, closestIntersection);

        GeneralPath combinedPath = new GeneralPath();
        combinedPath.append(circle, false);
        combinedPath.append(angleLine, false);

        double radiusDistance = ShapeUtilities.getScreenCircleRadius(worldToScreen, position, closestIntersection);

        DrawUtilities.drawCompositeFigure(g, worldToScreen, position, combinedPath,
                radiusDistance * 2.0d, Color.WHITE, paintMultiplier);

        Shape baseCircleTransformed = ShapeUtilities.ensureDynamicScaleShape(worldToScreen,
                position, circle, 12);
        DrawUtilities.drawOutlined(g, baseCircleTransformed, color, false);
    }

    @SuppressWarnings({"BooleanParameter", "WeakerAccess"})
    public static void drawOutlined(Graphics2D g, Shape shape, Paint color, boolean quality,
                                    Stroke outlineStroke, Stroke coreStroke) {
        RenderingHints originalHints = g.getRenderingHints();
        if (quality) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        g.setStroke(outlineStroke);
        g.setPaint(Color.BLACK);
        g.draw(shape);
        g.setStroke(coreStroke);
        g.setPaint(color);
        g.draw(shape);

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
        g.setRenderingHints(originalHints);
    }

    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Point2D screenPoint) {
        return DrawUtilities.paintScreenTextOutlined(g, text, null, screenPoint);
    }

    @SuppressWarnings("WeakerAccess")
    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Font fontInput, Point2D screenPoint) {
        return DrawUtilities.paintScreenTextOutlined(g, text, fontInput,
                null, screenPoint, RectangleCorner.BOTTOM_RIGHT);
    }

    /**
     * Note: this method is for painting in screen coordinates only.
     * @param screenPoint desired position of painted String.
     * @param fontInput if null, default value is Orbitron 14.
     * @param strokeInput if null, default value is 2.5f with rounded caps and joins.
     * @param corner determines what corner of painted text's bounding box will correspond to passed screen position.
     * E.g. if BOTTOM_RIGHT, the label will be painted to the upper left of screen point.
     * @return resulting {@link Shape} instance of bounds of the drawn text, from which bounding box positions can be retrieved.
     */

    public static Shape paintScreenTextOutlined(Graphics2D g, String text, Font fontInput, Stroke strokeInput,
                                                Point2D screenPoint, RectangleCorner corner) {
        Font font = fontInput;
        if (font == null) {
            font = Utility.getOrbitron(14);
        }

        GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
        Shape textShape = glyphVector.getOutline();

        Rectangle2D bounds = textShape.getBounds2D();
        bounds.setRect(screenPoint.getX(), screenPoint.getY(), bounds.getWidth(), bounds.getHeight());
        Point2D delta = ShapeUtilities.calculateCornerCoordinates(bounds, corner);
        double x = delta.getX();
        double y = delta.getY();

        Shape textShapeTranslated = ShapeUtilities.translateShape(textShape,x, y);
        Shape translatedBounds = ShapeUtilities.translateShape(glyphVector.getLogicalBounds(),x, y);

        DrawUtilities.paintOutlinedText(g, translatedBounds, textShapeTranslated, strokeInput);

        return ShapeUtilities.translateShape(glyphVector.getVisualBounds(),x, y);
    }

    private static void paintOutlinedText(Graphics2D g, Shape bounds, Shape textShapeTransformed,
                                          Stroke strokeInput) {
        Color fillColor = Color.WHITE;
        DrawUtilities.paintOutlinedText(g,  bounds, textShapeTransformed, strokeInput, fillColor);
    }

    /**
     * @param bounds will be used to draw shaded background of text.
     * @param strokeInput if null, default BasicStroke of 2.5 will be used, with round caps and joins.
     */
    public static void paintOutlinedText(Graphics2D g, Shape bounds, Shape textShapeTransformed,
                                         Stroke strokeInput, Paint fillColor) {
        Color outlineColor = Color.BLACK;

        Stroke stroke = strokeInput;
        if (stroke == null) {
            stroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        RenderingHints originalHints = g.getRenderingHints();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        DrawUtilities.fillShape(g, bounds, ColorUtilities.setColorAlpha(outlineColor, 50));
        DrawUtilities.outlineShape(g, textShapeTransformed, outlineColor, stroke, DrawMode.QUALITY);
        DrawUtilities.fillShape(g,textShapeTransformed, fillColor);

        g.setRenderingHints(originalHints);
    }

    /**
     * Draws the specified graphics action with conditional opacity based on the zoom level.
     * The opacity of the graphics action is adjusted according to the zoom level,
     * so that it is fully transparent (invisible) when the zoom level is 20 or below,
     * and gradually becomes more opaque as the zoom level increases above 20 until it reaches
     * fully opaque (alpha 1.0) when the zoom level exceeds 40.
     *
     * @param action The GraphicsAction object representing the graphics action to be drawn.
     *               The drawing behavior should not rely on the current alpha composite settings,
     *               as they will be temporarily adjusted within this method.
     */
    public static void drawWithConditionalOpacity(Graphics2D g, GraphicsAction action) {
        double zoomLevel = StaticController.getZoomLevel();

        double alpha;
        if (zoomLevel > 20) {
            alpha = (zoomLevel - 20.0) / 20.0;
            alpha = Math.min(alpha, 1.0);
        } else return;

        Composite old = Utility.setAlphaComposite(g, alpha);

        action.draw(g);

        g.setComposite(old);
    }

    public static void drawWithRotationTransform(Graphics2D g, AffineTransform worldToScreen,
                                                 Point2D anchor, double radiansRotation, GraphicsAction action) {
        AffineTransform oldAT = g.getTransform();
        AffineTransform oldWtS = new AffineTransform(worldToScreen);
        AffineTransform rotateInstance = AffineTransform.getRotateInstance(radiansRotation,
                anchor.getX(), anchor.getY());
        worldToScreen.concatenate(rotateInstance);

        g.transform(worldToScreen);

        action.draw(g);

        worldToScreen.setTransform(oldWtS);
        g.setTransform(oldAT);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    public static void drawCompositeFigure(Graphics2D g, AffineTransform worldToScreen, Point2D position, Shape figure,
                                           double measurement, Paint color, double paintMultiplier) {
        Shape transformed = ShapeUtilities.ensureSpecialScaleShape(worldToScreen,
                position, figure, 12 * paintMultiplier, measurement);
        DrawUtilities.drawOutlined(g, transformed, color, false,
                new BasicStroke(3.0f), new BasicStroke(2.25f));
    }

    public static void drawEntityCenterCross(Graphics2D g, AffineTransform worldToScreen,
                                             Point2D position, Paint crossColor) {
        Shape cross = ShapeUtilities.createPerpendicularCross(position, 0.4f);
        Shape transformedCross = ShapeUtilities.ensureDynamicScaleShape(worldToScreen,
                position, cross, 12);

        DrawUtilities.drawOutlined(g, transformedCross, crossColor);
    }

    public static void paintInstallableGhost(Graphics2D g, AffineTransform worldToScreen,
                                             double rotation, Point2D targetLocation,
                                             Sprite sprite)  {
        if (sprite == null) {
            return;
        }

        AffineTransform oldAT = g.getTransform();

        var spriteImage = sprite.getImage();
        int width = spriteImage.getWidth();
        int height = spriteImage.getHeight();

        AffineTransform transform = new AffineTransform(worldToScreen);
        double rotationRadians = Math.toRadians(rotation);

        double targetLocationX = targetLocation.getX();
        double targetLocationY = targetLocation.getY();

        Point2D difference = Utility.getSpriteCenterDifferenceToAnchor(spriteImage);
        Point2D anchorForSpriteCenter = new Point2D.Double(targetLocationX - difference.getX(),
                targetLocationY - difference.getY());

        double centerX = anchorForSpriteCenter.getX() + (double) width / 2;
        double centerY = anchorForSpriteCenter.getY() + (double) height / 2;
        AffineTransform rotationTransform = AffineTransform.getRotateInstance(rotationRadians,
                centerX, centerY);
        transform.concatenate(rotationTransform);

        g.transform(transform);

        float alpha = 0.5f;
        Composite old = Utility.setAlphaComposite(g, alpha);

        g.drawImage(spriteImage, (int) anchorForSpriteCenter.getX(),
                (int) anchorForSpriteCenter.getY(), width, height, null);

        g.setComposite(old);
        g.setTransform(oldAT);
    }

    public static void drawSpriteBorders(Graphics2D g, AffineTransform worldToScreen,
                                         RenderedImage sprite, Point2D anchor) {
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        Shape spriteBorder = new Rectangle2D.Double(anchor.getX(), anchor.getY(), width, height);
        Shape transformed = worldToScreen.createTransformedShape(spriteBorder);

        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        g.setStroke(new BasicStroke(3));
        g.setPaint(Color.BLACK);
        g.draw(transformed);
        g.setStroke(oldStroke);
        g.setPaint(Color.WHITE);
        g.draw(transformed);

        g.setPaint(oldPaint);
    }

    public static void drawSpriteCenter(Graphics2D g, AffineTransform worldToScreen,
                                         Point2D positionWorld) {
        double worldSize = 0.5;
        double thickness = 0.05;
        double screenSize = 12;
        DrawingParameters parameters = DrawingParameters.builder()
                .withWorldSize(worldSize)
                .withWorldThickness(thickness)
                .withScreenSize(screenSize)
                .withPaintColor(Color.BLACK)
                .build();
        DrawUtilities.drawDynamicCross(g, worldToScreen, positionWorld, parameters);
    }

}
