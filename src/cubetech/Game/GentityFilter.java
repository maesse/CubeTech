/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.Game;

/**
 *
 * @author mads
 */
public class GentityFilter {
    public static final IGentityFilter CLASSNAME = new IGentityFilter() {
        public boolean filter(Gentity test, String match) {
            return (test.classname.equalsIgnoreCase((String)match));
        }
    };

    public static final IGentityFilter TARGETNAME = new IGentityFilter() {
        public boolean filter(Gentity test, String match) {
                return (test.targetname.equalsIgnoreCase((String)match));
        }
    };
}
