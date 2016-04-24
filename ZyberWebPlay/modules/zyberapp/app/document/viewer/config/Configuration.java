package document.viewer.config;

import com.groupdocs.viewer.config.IServiceConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alex Bobkov
 */
public class Configuration implements IServiceConfiguration {
    private Properties properties;
    
    public Configuration(){
        if(properties == null){
            properties = new Properties();
            InputStream is = null;
            try {
                // Load property file
                is = getClass().getClassLoader().getResourceAsStream("application.properties");
                properties.load(is);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                try {
                    if(is != null){
                        is.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public String getFileDisplayName() {
        return properties.getProperty("groupdocs.viewer.fileDisplayName");
    }

    @Override
    public String getJqueryFileDownloadCookieName() {
        return properties.getProperty("groupdocs.viewer.jqueryFileDownloadCookieName");
    }

    @Override
    public String getLicensePath() {
        String licensePath = properties.getProperty("groupdocs.viewer.licensePath");
        if (licensePath == null || licensePath.isEmpty()) {
            return System.getenv("GROUPDOCS_VIEWER");
        }
        return licensePath;
    }

    @Override
    public String getApplicationPath() {
        return properties.getProperty("groupdocs.viewer.applicationPath");
    }

    @Override
    public String getBasePath() {
        return properties.getProperty("groupdocs.viewer.filePath");
    }

    @Override
    public Integer getWatermarkFontSize() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.watermarkFontSize"));
    }

    @Override
    public String getWatermarkPosition() {
        return properties.getProperty("groupdocs.viewer.watermarkPosition");
    }

    @Override
    public Boolean isConvertWordDocumentsCompletely() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.convertWordDocumentsCompletely"));
    }

    @Override
    public Boolean isIgnoreDocumentAbsence() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.ignoreDocumentAbsence"));
    }

    @Override
    public Boolean isPreloadPagesOnBrowserSide() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.preloadPagesOnBrowserSide"));
    }

    @Override
    public Boolean isPrintWithWatermark() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.printWithWatermark"));
    }

    @Override
    public Boolean isSupportPageRotation() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.supportPageRotation"));
    }

    @Override
    public Boolean isUseCache() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useCache"));
    }
    
    @Override
    public Boolean isUseBrowserCache(){
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useBrowserCache"));
    }

    @Override
    public Integer getExpirationDate() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.expirationDate"));
    }

    @Override
    public String getEncryptionKey() {
        return properties.getProperty("groupdocs.viewer.encKey");
    }
    
    @Override
    public String getLocalesPath() {
        return properties.getProperty("groupdocs.viewer.localesPath");
    }

    @Override
    public Integer getQuality() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.quality"));
    }

    @Override
    public Boolean isShowThumbnails() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showThumbnails"));
    }

    @Override
    public Boolean isOpenThumbnails() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.openThumbnails"));
    }

    @Override
    public Integer getInitialZoom() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.initialZoom"));
    }

    @Override
    public Boolean isZoomToFitWidth() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.zoomToFitWidth"));
    }

    @Override
    public String getCustomDocumentCss() {
        return null;
    }

    @Override
    public boolean isUseVirtualScrolling() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useVirtualScrolling"));
    }

    @Override
    public Boolean isZoomToFitHeight() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.zoomToFitHeight"));
    }

    @Override
    public Integer getWidth() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.width"));
    }

    @Override
    public Integer getHeight() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.height"));
    }

    @Override
    public String getBackgroundColor() {
        return properties.getProperty("groupdocs.viewer.backgroundColor");
    }

    @Override
    public Boolean isShowFolderBrowser() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showFolderBrowser"));
    }

    @Override
    public Boolean isShowPrint() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showPrint"));
    }

    @Override
    public Boolean isShowDownload() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showDownload"));
    }

    @Override
    public Boolean isShowZoom() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showZoom"));
    }

    @Override
    public Boolean isShowPaging() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showPaging"));
    }

    @Override
    public Boolean isShowViewerStyleControl() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showViewerStyleControl"));
    }

    @Override
    public Boolean isShowSearch() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showSearch"));
    }

    @Override
    public Integer getPreloadPagesCount() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.preloadPagesCount"));
    }

    @Override
    public Integer getViewerStyle() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.viewerStyle"));
    }

    @Override
    public Boolean isSupportTextSelection() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.supportTextSelection"));
    }

    @Override
    public Boolean isUsePdfPrinting() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.usePdfPrinting"));
    }

    @Override
    public Boolean isPdfPrintMarginsEnabled() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.pdfPrintMarginsEnabled"));
    }

    @Override
    public String getToolbarButtonsBoxShadowStyle() {
        return properties.getProperty("groupdocs.viewer.toolbarButtonsBoxShadowStyle");
    }

    @Override
    public String getToolbarButtonsBoxShadowHoverStyle() {
        return properties.getProperty("groupdocs.viewer.toolbarButtonsBoxShadowHoverStyle");
    }

    @Override
    public String getThumbnailsContainerBackgroundColor() {
        return properties.getProperty("groupdocs.viewer.thumbnailsContainerBackgroundColor");
    }

    @Override
    public String getThumbnailsContainerBorderRightColor() {
        return properties.getProperty("groupdocs.viewer.thumbnailsContainerBorderRightColor");
    }

    @Override
    public String getToolbarBorderBottomColor() {
        return properties.getProperty("groupdocs.viewer.toolbarBorderBottomColor");
    }

    @Override
    public String getToolbarInputFieldBorderColor() {
        return properties.getProperty("groupdocs.viewer.toolbarInputFieldBorderColor");
    }

    @Override
    public String getToolbarButtonBorderColor() {
        return properties.getProperty("groupdocs.viewer.toolbarButtonBorderColor");
    }

    @Override
    public String getToolbarButtonBorderHoverColor() {
        return properties.getProperty("groupdocs.viewer.toolbarButtonBorderHoverColor");
    }

    @Override
    public Integer getThumbnailsContainerWidth() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.thumbnailsContainerWidth"));
    }

    @Override
    public Boolean isShowDownloadErrorsInPopup() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showDownloadErrorsInPopup"));
    }

    @Override
    public Boolean isShowImageWidth() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showImageWidth"));
    }

    @Override
    public Boolean isShowHeader() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showHeader"));
    }

    @Override
    public Integer getMinimumImageWidth() {
        return Integer.valueOf(properties.getProperty("groupdocs.viewer.minimumImageWidth"));
    }

    @Override
    public Boolean isEnableStandardErrorHandling() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.enableStandardErrorHandling"));
    }

    @Override
    public Boolean isUseHtmlBasedEngine() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useHtmlBasedEngine"));
    }

    @Override
    public Boolean isUseImageBasedPrinting() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useImageBasedPrinting"));
    }

    @Override
    public Boolean isDownloadPdfFile() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.downloadPdfFile"));
    }

    @Override
    public Boolean isSearchForSeparateWords() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.searchForSeparateWords"));
    }

    @Override
    public Boolean isPreventTouchEventsBubbling() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.preventTouchEventsBubbling"));
    }

    @Override
    public Boolean isUseInnerThumbnails() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useInnerThumbnails"));
    }

    @Override
    public String getWatermarkText() {
        return properties.getProperty("groupdocs.viewer.watermarkText");
    }

    @Override
    public String getWatermarkColor() {
        return properties.getProperty("groupdocs.viewer.watermarkColor");
    }

    @Override
    public Boolean isSupportPageReordering() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.supportPageReordering"));
    }

    @Override
    public Boolean isOnlyShrinkLargePages() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.onlyShrinkLargePages"));
    }

    @Override
    public String getSearchHighlightColor() {
        return properties.getProperty("groupdocs.viewer.searchHighlightColor");
    }

    @Override
    public String getCurrentSearchHighlightColor() {
        return properties.getProperty("groupdocs.viewer.currentSearchHighlightColor");
    }

    @Override
    public Boolean isTreatPhrasesInDoubleQuotesAsExactPhrases() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.treatPhrasesInDoubleQuotesAsExactPhrases"));
    }

    @Override
    public Boolean isUsePngImagesForHtmlBasedEngine() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.usePngImagesForHtmlBasedEngine"));
    }

    @Override
    public Boolean isShowOnePageInRow() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.showOnePageInRow"));
    }

    @Override
    public Boolean isLoadAllPagesOnSearch() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.loadAllPagesOnSearch"));
    }

    @Override
    public Boolean isUseEmScaling() {
        return Boolean.valueOf(properties.getProperty("groupdocs.viewer.useEmScaling"));
    }

    @Override
    public String getUploadPath() {
        return properties.getProperty("groupdocs.viewer.uploadPath");
    }

    @Override
    public Long getMaxCacheSize() {
        return Long.valueOf(properties.getProperty("groupdocs.viewer.maxCacheSize"));
    }

    @Override
    public String getCachePath() {
        return properties.getProperty("groupdocs.viewer.cachePath");
    }

    @Override
    public String getEncoding() {
        return properties.getProperty("groupdocs.viewer.encoding");
    }

    @Override
    public String getFontsPath() {
        return null;
    }

    @Override
    public Integer getImageWebRequestTimeout() {
        return 5000;
    }

    @Override
    public Integer getImageResolution() {
        return null;
    }

    @Override
    public Boolean isShowHiddenSlides() {
        return false;
    }
}
