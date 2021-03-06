package com.thomsonreuters.lsps.transmart.etl

import com.thomsonreuters.lsps.transmart.files.GplFile

/**
 * Created by bondarev on 3/28/14.
 */
class PlatformProcessor {
    static long eachPlatformEntry(File platformFile, config, Closure processEntry) {
        long lineNum = 0
        GplFile gplFile = new GplFile(platformFile)
        int entrezGeneIdIdx = -1, geneSymbolIdx = -1, speciesIdx = -1
        def header = gplFile.header
        header.eachWithIndex { String val, int idx ->
            if (val ==~ /(?i)(ENTREZ[\s_]*)*GENE([\s_]*ID)*/) entrezGeneIdIdx = idx
            else if (val ==~ /(?i)(GENE[\s_]*)*SYMBOL/) geneSymbolIdx = idx
            else if (val ==~ /(?i)SPECIES([\s_]*SCIENTIFIC)([\s_]*NAME)/) speciesIdx = idx
        }
        if (speciesIdx == -1) {
            // OK, trying to get species from the description
            config.logger.log(LogType.WARNING, "Species not found in the platform file, using description")
        }
        if (entrezGeneIdIdx == -1 || geneSymbolIdx == -1) {
            throw new Exception("Incorrect platform file header")
        }
        config.logger.log(LogType.DEBUG, "ENTREZ, SYMBOL, SPECIES => " +
                "${header[entrezGeneIdIdx]}, " +
                "${header[geneSymbolIdx]}, " +
                "${speciesIdx != -1 ? header[speciesIdx] : '(Not specified)'}")

        gplFile.eachEntry { String[] cols ->
            lineNum++

            String origId = cols[entrezGeneIdIdx]
            // In previous versions we completely ignored such rows
            if (!config?.useFirstGeneId && !origId.isEmpty() && !(origId ==~ /\d+/)) {
                return
            }
            def (String entrezId, String geneSymbol) =
                normalizeGeneIdAndSymbol(origId, cols[geneSymbolIdx], config)
            config.logger.log(LogType.PROGRESS, "[${lineNum}]")
            processEntry([
                    probeset_id   : cols[0],
                    gene_symbol   : geneSymbol,
                    entrez_gene_id: entrezId,
                    species       : speciesIdx != -1 ? cols[speciesIdx] : null
            ])
        }
        config.logger.log(LogType.PROGRESS, "")
        return lineNum
    }

    // We decided on 2015-07-17 that if Entrez Gene Id looks like "123 /// 456"
    // we take the first number only. In this case we also take only the part
    // of gene symbol that precedes '///' (if any). Finally if gene symbol does
    // not make any sense at all (e.g.: "---") we ignore it.
    static String[] normalizeGeneIdAndSymbol(String entrezId, String geneSymbol, config) {
        String normalizedId = config?.useFirstGeneId ? entrezId.trim().replaceFirst(' *//+.*', '') : entrezId
        if (normalizedId != entrezId) {
            geneSymbol = geneSymbol.replaceFirst(' *//+.*', '')
        }
        // Ignore any Entrez Gene Ids that do not look like positive integer.
        if (normalizedId.isEmpty() || !(normalizedId ==~ /\d+/)) {
            normalizedId = null
        }
        if (!(geneSymbol =~ /\w/)) {
            geneSymbol = ''
        }
        return [normalizedId, geneSymbol]
    }
}
