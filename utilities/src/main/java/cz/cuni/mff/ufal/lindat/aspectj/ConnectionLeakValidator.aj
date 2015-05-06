package cz.cuni.mff.ufal.lindat.aspectj;

public aspect ConnectionLeakValidator {

	pointcut beginSession : execution (* cz.cuni.mff.ufal.lindat.utilities.HibernateFunctionalityManager.openSession(..));
	pointcut endSession : execution (* cz.cuni.mff.ufal.lindat.utilities.HibernateFunctionalityManager.closeSession(..));
	
	after() returning() : beginSession() {
		
	}
	
	after() returning() : endSession() {
		
	} 
	 
	
}
