package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// what an approver says about a job. which of the three answers is being given is the route and
// never a field - approving, refusing and asking for changes are three different things to do to
// a company's work, and one of them puts the job in front of workers
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobReviewRequest {

    // optional on an approval and read on every answer. an approver who waved a job through with
    // a note about the rate has said something the employer is better off reading
    private String comments;

    // required on a refusal and on a request for changes. a company told only "no" has to guess
    // at what to fix, and guesses come back through the same queue
    private String reason;
}
