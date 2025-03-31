package ru.l0sty.frogdisplays.util;

public final class NetworkUtil {
//
//    private static final CinemaModClient CD = CinemaModClient.getInstance();
//    /* INCOMING */
//    private static final Identifier CHANNEL_SCREENS = Identifier.of(CinemaMod.MODID, "screens");
//    private static final Identifier CHANNEL_LOAD_SCREEN = Identifier.of(CinemaMod.MODID, "load_screen");
//    private static final Identifier CHANNEL_UNLOAD_SCREEN = Identifier.of(CinemaMod.MODID, "unload_screen");
//    private static final Identifier CHANNEL_UPDATE_PREVIEW_SCREEN = Identifier.of(CinemaMod.MODID, "update_preview_screen");
//    private static final Identifier CHANNEL_OPEN_SETTINGS_SCREEN = Identifier.of(CinemaMod.MODID, "open_settings_screen");
//    private static final Identifier CHANNEL_OPEN_HISTORY_SCREEN = Identifier.of(CinemaMod.MODID, "open_history_screen");
//    private static final Identifier CHANNEL_OPEN_PLAYLISTS_SCREEN = Identifier.of(CinemaMod.MODID, "open_playlists_screen");
//    private static final Identifier CHANNEL_VIDEO_LIST_HISTORY_SPLIT = Identifier.of(CinemaMod.MODID, "video_list_history_split");
//    private static final Identifier CHANNEL_VIDEO_LIST_PLAYLIST_SPLIT = Identifier.of(CinemaMod.MODID, "video_list_playlist_split");
//    private static final Identifier CHANNEL_VIDEO_QUEUE_STATE = Identifier.of(CinemaMod.MODID, "video_queue_state");
//    /* OUTGOING */
//    private static final Identifier CHANNEL_VIDEO_REQUEST = Identifier.of(CinemaMod.MODID, "video_request");
//    private static final Identifier CHANNEL_VIDEO_HISTORY_REMOVE = Identifier.of(CinemaMod.MODID, "video_history_remove");
//    private static final Identifier CHANNEL_VIDEO_QUEUE_VOTE = Identifier.of(CinemaMod.MODID, "video_queue_vote");
//    private static final Identifier CHANNEL_VIDEO_QUEUE_REMOVE = Identifier.of(CinemaMod.MODID, "video_queue_remove");
//    private static final Identifier CHANNEL_SHOW_VIDEO_TIMELINE = Identifier.of(CinemaMod.MODID, "show_video_timeline");
//
//    public static void registerReceivers() {
//        ClientPlayNetworking.registerGlobalReceiver(new VideoService().getId(), (payload, context) -> {
//                CD.getVideoServiceManager().register(payload);
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_SCREENS, (client, handler, buf, responseSender) -> {
//            int length = buf.readInt();
//            for (int i = 0; i < length; i++)
//                CD.getScreenManager().registerScreen(new Screen().fromBytes(buf));
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_LOAD_SCREEN, (client, handler, buf, responseSender) -> {
//            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
//            Screen screen = CD.getScreenManager().getScreen(pos);
//            if (screen == null) return;
//            Video video = new Video().fromBytes(buf);
//            client.submit(() -> screen.loadVideo(video));
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_UNLOAD_SCREEN, (client, handler, buf, responseSender) -> {
//            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
//            Screen screen = CD.getScreenManager().getScreen(pos);
//            if (screen == null) return;
//            client.submit(screen::closeBrowser);
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_UPDATE_PREVIEW_SCREEN, (client, handler, buf, responseSender) -> {
//            PreviewScreenManager manager = CD.getPreviewScreenManager();
//            PreviewScreen previewScreen = new PreviewScreen().fromBytes(buf);
//            VideoInfo videoInfo = buf.readBoolean() ? new VideoInfo().fromBytes(buf) : null;
//            previewScreen.setVideoInfo(videoInfo);
//            if (manager.getPreviewScreen(previewScreen.getBlockPos()) == null)
//                manager.addPreviewScreen(previewScreen);
//            else
//                manager.getPreviewScreen(previewScreen.getBlockPos()).setVideoInfo(videoInfo);
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_VIDEO_LIST_HISTORY_SPLIT, (client, handler, buf, responseSender) -> {
//            List<VideoListEntry> entries = new ArrayList<>();
//            int length = buf.readInt();
//            for (int i = 0; i < length; i++)
//                entries.add(new VideoListEntry().fromBytes(buf));
//            CD.getVideoListManager().getHistory().merge(new VideoList(entries));
//        });
//        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_VIDEO_QUEUE_STATE, (client, handler, buf, responseSender) -> {
//            CD.getVideoQueue().fromBytes(buf);
//            client.submit(() -> {
//                if (client.currentScreen instanceof VideoQueueScreen) {
//                    VideoQueueScreen videoQueueScreen = (VideoQueueScreen) client.currentScreen;
//                    videoQueueScreen.videoQueueWidget.update();
//                }
//            });
//        });
//    }
//
//    public static void sendVideoRequestPacket(VideoInfo videoInfo) {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        videoInfo.toBytes(buf);
//        ClientPlayNetworking.send(CHANNEL_VIDEO_REQUEST, buf);
//    }
//
//    public static void sendDeleteHistoryPacket(VideoInfo videoInfo) {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        videoInfo.toBytes(buf);
//        ClientPlayNetworking.send(CHANNEL_VIDEO_HISTORY_REMOVE, buf);
//    }
//
//    public static void sendVideoQueueVotePacket(VideoInfo videoInfo, int voteType) {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        videoInfo.toBytes(buf);
//        buf.writeInt(voteType);
//        ClientPlayNetworking.send(CHANNEL_VIDEO_QUEUE_VOTE, buf);
//    }
//
//    public static void sendVideoQueueRemovePacket(VideoInfo videoInfo) {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        videoInfo.toBytes(buf);
//        ClientPlayNetworking.send(CHANNEL_VIDEO_QUEUE_REMOVE, buf);
//    }
//
//    public static void sendShowVideoTimelinePacket() {
//        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//        ClientPlayNetworking.send(CHANNEL_SHOW_VIDEO_TIMELINE, buf);
//    }

}
