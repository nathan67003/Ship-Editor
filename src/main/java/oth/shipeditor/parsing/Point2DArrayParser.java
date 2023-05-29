package oth.shipeditor.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * @author Ontheheavens
 * @since 05.05.2023
 */
public class Point2DArrayParser extends JsonDeserializer<Point2D[]> {

    @Override
    public Point2D.Double[] deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        int size = node.size() / 2;
        Point2D.Double[] points = new Point2D.Double[size];
        for (int i = 0; i < size; i++) {
            int x = node.get(i * 2).asInt();
            int y = node.get(i * 2 + 1).asInt();
            points[i] = new Point2D.Double(x, y);
        }
        return points;
    }

}