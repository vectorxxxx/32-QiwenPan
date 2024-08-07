package com.qiwenshare.file.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author VectorX
 * @version V1.0
 * @description
 * @date 2024-08-07 09:12:45
 */
@AllArgsConstructor
@Getter
public enum FileExtendTemplatePathEnum
{
    DOCX("docx", "template/Word.docx"),
    XLSX("xlsx", "template/Excel.xlsx"),
    PPTX("pptx", "template/PowerPoint.pptx"),
    TXT("txt", "template/Text.txt"),
    DRAWIO("drawio", "template/Drawio.drawio");

    private String extendName;
    private String templateFilePath;

    /**
     * 获取模板文件路径
     *
     * @param extendName 扩展名称
     * @return {@link String }
     */
    public static String getTemplateFilePath(String extendName) {
        for (FileExtendTemplatePathEnum fileExtendTemplatePathEnum : FileExtendTemplatePathEnum.values()) {
            if (fileExtendTemplatePathEnum
                    .getExtendName()
                    .equals(extendName)) {
                return fileExtendTemplatePathEnum.getTemplateFilePath();
            }
        }
        return null;
    }
}
