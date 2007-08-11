/**
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipientís reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regentsí employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.core.tests.demandpa;

import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.refinepolicy.AlwaysRefineFieldsPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class RefineFieldsPtrTest extends AbstractPtrTest {

  public void testNastyPtrs() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_NASTY_PTRS, 10);
  }

  public void testGlobal() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_GLOBAL, 1);
  }

  public void testFields() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_FIELDS, 1);
  }

  public void testFieldsHarder() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_FIELDS_HARDER, 1);
  }

  public void testArrays() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAYS, 2);
  }

  public void testGetterSetter() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_GETTER_SETTER, 1);
  }

  public void testArraySet() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAY_SET, 2);
  }

  public void testArraySetIter() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_ARRAY_SET_ITER, 2);
  }

  public void testMultiDim() throws ClassHierarchyException {
    doPointsToSizeTest(TestInfo.SCOPE_FILE, TestInfo.TEST_MULTI_DIM, 2);
  }

  @Override
  public DemandRefinementPointsTo makeDemandPointerAnalysis(String scopeFile, String mainClass) throws ClassHierarchyException {
    DemandRefinementPointsTo dmp = super.makeDemandPointerAnalysis(scopeFile, mainClass);
    dmp
        .setRefinementPolicyFactory(new SinglePassRefinementPolicy.Factory(new AlwaysRefineFieldsPolicy(),
            new NeverRefineCGPolicy()));
    return dmp;
  }

}
