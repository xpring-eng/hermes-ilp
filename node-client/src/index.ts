import { IlpServiceClient } from "../generated/ilp_as_a_service_grpc_pb";
import { credentials } from "grpc";
import { AccountId } from "../generated/accounts_pb";

/* eslint-disable @typescript-eslint/no-floating-promises */
/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/require-await */

async function runILPTest(): Promise<void> {
  const client = new IlpServiceClient(
    "127.0.0.1:6565",
    credentials.createInsecure()
  );

  const req = new AccountId();
  client.getBalance(req, (error, response): void => {
    if (error != null || response == null) {
      console.log("ERRORED :(");
      console.log(error);
      return;
    }
    console.log(JSON.stringify(response));
  });
}

runILPTest();
