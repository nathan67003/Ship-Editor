package oth.shipeditor.representation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import oth.shipeditor.parsing.deserialize.ColorArrayRGBADeserializer;
import oth.shipeditor.parsing.serialize.ColorArrayRGBASerializer;

import java.awt.*;

/**
 * @author Ontheheavens
 * @since 16.07.2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class HullStyle {

    @Setter
    private String hullStyleID;

    @JsonDeserialize(using = ColorArrayRGBADeserializer.class)
    @JsonSerialize(using = ColorArrayRGBASerializer.class)
    @JsonProperty("shieldRingColor")
    private Color shieldRingColor;

    @JsonDeserialize(using = ColorArrayRGBADeserializer.class)
    @JsonSerialize(using = ColorArrayRGBASerializer.class)
    @JsonProperty("shieldInnerColor")
    private Color shieldInnerColor;

    public HullStyle() {
        hullStyleID = "DEFAULT";
        shieldRingColor = Color.WHITE;
        shieldInnerColor = new Color(0x4D00AFFA, true);
    }

}
