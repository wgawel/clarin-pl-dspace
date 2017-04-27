--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

------------------------------------------------------
-- DS-ClarinPL_Token introduced new field for eperson clarin token
------------------------------------------------------
ALTER TABLE eperson ADD clarin_token_id varchar(255);