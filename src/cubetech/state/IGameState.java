/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.state;

/**
 *
 * @author mads
 */
public interface IGameState {
    public void Enter();
    public void Exit();
    public void RunFrame(int msec);
    public String GetName();
}
