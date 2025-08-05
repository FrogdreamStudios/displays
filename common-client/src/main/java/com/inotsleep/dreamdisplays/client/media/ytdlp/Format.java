package com.inotsleep.dreamdisplays.client.media.ytdlp;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class Format {

    @SerializedName("container")
    private String container;

    @SerializedName("asr")
    private Integer asr;

    @SerializedName("aspect_ratio")
    private String aspectRatio;

    @SerializedName("__needs_testing")
    private Boolean needsTesting;

    @SerializedName("audio_ext")
    private String audioExt;

    @SerializedName("tbr")
    private Double tbr;

    @SerializedName("columns")
    private Integer columns;

    @SerializedName("vbr")
    private Double vbr;

    @SerializedName("source_preference")
    private Integer sourcePreference;

    @SerializedName("language")
    private String language;

    @SerializedName("filesize")
    private Long filesize;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("format_index")
    private Integer formatIndex;

    @SerializedName("protocol")
    private String protocol;

    @SerializedName("format_note")
    private String formatNote;

    @SerializedName("format_id")
    private String formatId;

    @SerializedName("manifest_url")
    private String manifestUrl;

    @SerializedName("fragments")
    private List<Object> fragments;

    @SerializedName("downloader_options")
    private Map<String, Object> downloaderOptions;

    @SerializedName("height")
    private Integer height;

    @SerializedName("ext")
    private String ext;

    @SerializedName("vcodec")
    private String vcodec;

    @SerializedName("video_ext")
    private String videoExt;

    @SerializedName("audio_channels")
    private Integer audioChannels;

    @SerializedName("preference")
    private Integer preference;

    @SerializedName("format")
    private String format;

    @SerializedName("fps")
    private Double fps;

    @SerializedName("dynamic_range")
    private String dynamicRange;

    @SerializedName("rows")
    private Integer rows;

    @SerializedName("language_preference")
    private Integer languagePreference;

    @SerializedName("url")
    private String url;

    @SerializedName("quality")
    private Integer quality;

    @SerializedName("abr")
    private Double abr;

    @SerializedName("http_headers")
    private Map<String, String> httpHeaders;

    @SerializedName("has_drm")
    private Boolean hasDrm;

    @SerializedName("width")
    private Integer width;

    @SerializedName("acodec")
    private String acodec;

    @SerializedName("filesize_approx")
    private Long filesizeApprox;

    // ===== Getters & Setters =====
    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }

    public Integer getAsr() { return asr; }
    public void setAsr(Integer asr) { this.asr = asr; }

    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }

    public Boolean getNeedsTesting() { return needsTesting; }
    public void setNeedsTesting(Boolean needsTesting) { this.needsTesting = needsTesting; }

    public String getAudioExt() { return audioExt; }
    public void setAudioExt(String audioExt) { this.audioExt = audioExt; }

    public Double getTbr() { return tbr; }
    public void setTbr(Double tbr) { this.tbr = tbr; }

    public Integer getColumns() { return columns; }
    public void setColumns(Integer columns) { this.columns = columns; }

    public Double getVbr() { return vbr; }
    public void setVbr(Double vbr) { this.vbr = vbr; }

    public Integer getSourcePreference() { return sourcePreference; }
    public void setSourcePreference(Integer sourcePreference) { this.sourcePreference = sourcePreference; }

    public String getLanguage() { return language == null ? "none" : language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getFilesize() { return filesize; }
    public void setFilesize(Long filesize) { this.filesize = filesize; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Integer getFormatIndex() { return formatIndex; }
    public void setFormatIndex(Integer formatIndex) { this.formatIndex = formatIndex; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getFormatNote() { return formatNote; }
    public void setFormatNote(String formatNote) { this.formatNote = formatNote; }

    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }

    public String getManifestUrl() { return manifestUrl; }
    public void setManifestUrl(String manifestUrl) { this.manifestUrl = manifestUrl; }

    public List<Object> getFragments() { return fragments; }
    public void setFragments(List<Object> fragments) { this.fragments = fragments; }

    public Map<String, Object> getDownloaderOptions() { return downloaderOptions; }
    public void setDownloaderOptions(Map<String, Object> downloaderOptions) { this.downloaderOptions = downloaderOptions; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }

    public String getVcodec() { return vcodec; }
    public void setVcodec(String vcodec) { this.vcodec = vcodec; }

    public String getVideoExt() { return videoExt; }
    public void setVideoExt(String videoExt) { this.videoExt = videoExt; }

    public Integer getAudioChannels() { return audioChannels; }
    public void setAudioChannels(Integer audioChannels) { this.audioChannels = audioChannels; }

    public Integer getPreference() { return preference; }
    public void setPreference(Integer preference) { this.preference = preference; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Double getFps() { return fps; }
    public void setFps(Double fps) { this.fps = fps; }

    public String getDynamicRange() { return dynamicRange; }
    public void setDynamicRange(String dynamicRange) { this.dynamicRange = dynamicRange; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(Integer languagePreference) { this.languagePreference = languagePreference; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getQuality() { return quality; }
    public void setQuality(Integer quality) { this.quality = quality; }

    public Double getAbr() { return abr; }
    public void setAbr(Double abr) { this.abr = abr; }

    public Map<String, String> getHttpHeaders() { return httpHeaders; }
    public void setHttpHeaders(Map<String, String> httpHeaders) { this.httpHeaders = httpHeaders; }

    public Boolean getHasDrm() { return hasDrm; }
    public void setHasDrm(Boolean hasDrm) { this.hasDrm = hasDrm; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public String getAcodec() { return acodec; }
    public void setAcodec(String acodec) { this.acodec = acodec; }

    public Long getFilesizeApprox() { return filesizeApprox; }
    public void setFilesizeApprox(Long filesizeApprox) { this.filesizeApprox = filesizeApprox; }

    @Override
    public String toString() {
        return "Format{" +
                "container='" + container + '\'' +
                ", asr=" + asr +
                ", aspectRatio='" + aspectRatio + '\'' +
                ", needsTesting=" + needsTesting +
                ", audioExt='" + audioExt + '\'' +
                ", tbr=" + tbr +
                ", columns=" + columns +
                ", vbr=" + vbr +
                ", sourcePreference=" + sourcePreference +
                ", language='" + language + '\'' +
                ", filesize=" + filesize +
                ", resolution='" + resolution + '\'' +
                ", formatIndex=" + formatIndex +
                ", protocol='" + protocol + '\'' +
                ", formatNote='" + formatNote + '\'' +
                ", formatId='" + formatId + '\'' +
                ", manifestUrl='" + manifestUrl + '\'' +
                ", fragments=" + fragments +
                ", downloaderOptions=" + downloaderOptions +
                ", height=" + height +
                ", ext='" + ext + '\'' +
                ", vcodec='" + vcodec + '\'' +
                ", videoExt='" + videoExt + '\'' +
                ", audioChannels=" + audioChannels +
                ", preference=" + preference +
                ", format='" + format + '\'' +
                ", fps=" + fps +
                ", dynamicRange='" + dynamicRange + '\'' +
                ", rows=" + rows +
                ", languagePreference=" + languagePreference +
                ", url='" + url + '\'' +
                ", quality=" + quality +
                ", abr=" + abr +
                ", httpHeaders=" + httpHeaders +
                ", hasDrm=" + hasDrm +
                ", width=" + width +
                ", acodec='" + acodec + '\'' +
                ", filesizeApprox=" + filesizeApprox +
                '}';
    }
}
