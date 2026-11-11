// SPDX-License-Identifier: AGPL-3.0-or-later
package org.sg.pdfcolorhighlighter.domain

import org.junit.Assert.*
import org.junit.Test

class ExternalSavePresentationTest {
    private val a="a".repeat(64)
    private val b="b".repeat(64)
    private fun state(current:String?=a,source:String?=a,exported:String?=null,detached:Boolean=false,failed:Boolean=false,visible:Boolean=true)=
        ExternalSavePresentation.state(visible,current,source,exported,detached,failed)
    @Test fun noDocument(){assertEquals(ExternalSaveState.NO_DOCUMENT,state(visible=false))}
    @Test fun noDocumentOverridesFailure(){assertEquals(ExternalSaveState.NO_DOCUMENT,state(failed=true,visible=false))}
    @Test fun originalSaved(){assertEquals(ExternalSaveState.ORIGINAL_SAVED,state())}
    @Test fun modified(){assertEquals(ExternalSaveState.UNSAVED,state(current=b))}
    @Test fun exportSaved(){assertEquals(ExternalSaveState.COPY_SAVED,state(current=b,exported=b))}
    @Test fun editAfterExport(){assertEquals(ExternalSaveState.UNSAVED,state(current=b,source=null,exported=a))}
    @Test fun sourceWinsOverExport(){assertEquals(ExternalSaveState.ORIGINAL_SAVED,state(exported=a))}
    @Test fun recovered(){assertEquals(ExternalSaveState.RECOVERED,state(detached=true))}
    @Test fun recoveredExport(){assertEquals(ExternalSaveState.COPY_SAVED,state(exported=a,detached=true))}
    @Test fun recoveredMustNotClaimOriginalSaved(){assertNotEquals(ExternalSaveState.ORIGINAL_SAVED,state(detached=true))}
    @Test fun failure(){assertEquals(ExternalSaveState.WRITE_FAILED,state(current=b,failed=true))}
    @Test fun failedExportEvenWhenOriginalSaved(){assertEquals(ExternalSaveState.WRITE_FAILED,state(failed=true))}
    @Test fun missingHash(){assertEquals(ExternalSaveState.UNKNOWN,state(current=null))}
    @Test fun unrefreshedHash(){assertEquals(ExternalSaveState.UNKNOWN,state(current="unrefreshed:12"))}
    @Test fun malformedSavedHash(){assertEquals(ExternalSaveState.UNSAVED,state(source="bad"))}
    @Test fun undoToSavedBytes(){assertEquals(ExternalSaveState.UNSAVED,state(current=b));assertEquals(ExternalSaveState.ORIGINAL_SAVED,state(current=a));assertEquals(ExternalSaveState.UNSAVED,state(current=b))}
}
