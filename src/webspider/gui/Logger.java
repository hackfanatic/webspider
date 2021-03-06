package webspider.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import webspider.actions.SpiderActions;


/**
 * A globbal applicatin logger that outputs to
 * the graphical user interface as well as
 * to the command line
 * @author Shaabi Mohammed
 */
public class Logger {
    JTextArea log;
    JScrollPane scroll;

    Logger(Container frame, SpiderActions actions) {
        initLogger(actions);
        frame.add(scroll, BorderLayout.CENTER);
    }

    private void initLogger(SpiderActions actions){
        log = new JTextArea();
        scroll = new JScrollPane(log);
        
        log.setFont(Font.decode("Monospace-12"));
        log.setEditable(false);

        scroll.setAutoscrolls(true);

        actions.initScroll(scroll);
        actions.initLogger(log);
    }
}
