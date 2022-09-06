Commonly Used Git Commands:-

-git pull
-git push
-git checkout invoicing
-git reset hard
-git log --grep "CCEP-12345"   →  grep command to search commit # for Jira id
-git cherry-pick "commit id"
-git config --system core.longpaths true  →  to resolve Filename too long error: unable to create file" errors

-----------------------------------------------------------------------------------------------------------------------------------------------------

GITLAB

>> Checkout and Pull invoicing-gitlab
                git checkout invoicing-gitlab
                git pull
                
>> Create <<LocalBranch>> for Story or Defect 
                git checkout -b <<LocalBranch>> 
                git push --set-upstream origin <<LocalBranch>> 
                
>> Do the changes in the <<LocalBranch>>, build and commit the changes to <<LocalBranch>>.
                mvn clean install
                git commit -m "<<Commit Msg>>" 
>> Merge invoicing-gitlab to <<LocalBranch>> 
                1.            git checkout invoicing-gitlab
                2.            git pull
                3.            git checkout <<LocalBranch>> 
                4.            git pull
                5.            git merge invoicing-gitlab
                6.            git push --set-upstream origin <<LocalBranch>> 

// git pull origin invoicing-gitlab
>> Initiate 'New Merge Request' 
                Do from the UI

------------------------------------------------------------------------------------------------------------------------------------------------------
