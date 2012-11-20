package pr.sna.snaprkit;

public class StoppableThread extends Thread
{
    private boolean mStopFlag;

    public synchronized void requestStop()
    {
        this.mStopFlag = true;
    }

    public synchronized boolean hasStopBeenRequested()
    {
        return this.mStopFlag;
    }
}