package oth.shipeditor.components.logging;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 * @author Ontheheavens
 * @since 10.10.2023
 */
@Plugin(
        name = TextAreaAppender.DESIGNATION,
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class TextAreaAppender extends AbstractAppender {

    private static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} %-5level %logger{1} - %msg%n";

    static final String DESIGNATION = "TextAreaAppender";

    private final PatternLayout patternLayout;

    @SuppressWarnings("deprecation")
    protected TextAreaAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
        if (layout instanceof PatternLayout checked) {
            this.patternLayout = checked;
        } else {
            this.patternLayout = PatternLayout.newBuilder()
                    .withPattern(DEFAULT_PATTERN)
                    .build();
        }
    }

    @PluginFactory
    public static TextAreaAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new TextAreaAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        var textArea = LogsPanel.getLogger();
        if (textArea != null) {
            String formattedMessage = patternLayout.toSerializable(event);
            textArea.append(formattedMessage);
        }
    }
}