package cubetech;

import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public abstract class EnemyBase {
    private Vector2f Position = new Vector2f();
    private Vector2f Velocity = new Vector2f();
    public boolean RemoveMe = false;

    public EnemyBase(Vector2f spawnPosition) {
        Position.x = spawnPosition.x;
        Position.y = spawnPosition.y;
    }

    public abstract void Update(int msec, Vector2f PlayerPosition);
    public abstract void Render();
    public abstract void Die();

    public Vector2f getPosition() {
        return Position;
    }

    public void setPosition(Vector2f Position) {
        this.Position = Position;
    }

    public Vector2f getVelocity() {
        return Velocity;
    }

    public void setVelocity(Vector2f Velocity) {
        this.Velocity = Velocity;
    }
}
