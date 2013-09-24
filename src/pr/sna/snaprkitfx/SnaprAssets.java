package pr.sna.snaprkitfx;

import java.util.ArrayList;

import pr.sna.snaprkitfx.SnaprFilterUtil.FilterPack;
import pr.sna.snaprkitfx.SnaprStickerUtil.StickerPack;

public class SnaprAssets
{
	private FilterPack mFilterPack;
	private ArrayList<StickerPack> mStickerPacks;

	public SnaprAssets(FilterPack filterPack, ArrayList<StickerPack> stickerPacks)
	{
		mFilterPack = filterPack;
		mStickerPacks = stickerPacks;
	}
	
	public FilterPack getFilterPack()
	{
		return mFilterPack;
	}
	
	public void setFilterPack(FilterPack filterPack)
	{
		mFilterPack = filterPack;
	}
	
	public ArrayList<StickerPack> getStickerPacks()
	{
		return mStickerPacks;
	}
	
	public void setStickerPacks(ArrayList<StickerPack> stickerPacks)
	{
		mStickerPacks = stickerPacks;
	}
}
