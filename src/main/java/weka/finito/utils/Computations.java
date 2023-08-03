package weka.finito.utils;

import weka.core.Attribute;
import weka.core.Utils;

import java.rmi.RemoteException;

public class Computations {
    // Create a Compute Locally

    /*
    // Probably would be a NodeInfo object instead?
    private TreeNode formMajorityClassLeaf() throws RemoteException {
        // !!! FIX IMPORTANT
// IF DISTRIBUTION is all zeroes do something else
// For now just setting all classes to be equiprobable.. This is clearly wrong
        boolean nonZeroFlag = false;
        for (int i = 1; i<m_nhosts; i++)
            m_Slaves[i-1].prepareTransVector();

        m_Distribution = new double[m_insts.numClasses()];
        for (int i=0; i<m_insts.numClasses(); i++) {
            Attribute classattr = m_insts.classAttribute();
            prepareTransVectorWithClassFilter(classattr.indexOfValue(classattr.value(i)));
            m_Distribution[i] = SetIntersect();
            if (m_Distribution[i] > 0) nonZeroFlag = true;
        }

        if (!nonZeroFlag) {
            for (int i=0; i<m_insts.numClasses(); i++) {
                m_Distribution[i] = 1;
            }
        }

        Utils.normalize(m_Distribution);
        m_ClassValue = Utils.maxIndex(m_Distribution);

        return new LeafNode(m_ClassValue, m_Distribution);
    }
     */
}
