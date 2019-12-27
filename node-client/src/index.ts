import { IlpServiceClient } from "../generated/ilp_as_a_service_grpc_pb";
import { credentials } from "grpc";
import { GetBalanceRequest } from "../generated/get_balance_request_pb";
import { CreateAccountRequest } from "../generated/create_account_request_pb";

/* eslint-disable @typescript-eslint/no-floating-promises */
/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/require-await */

// async function runILPGetBalanceTest(): Promise<void> {
//   const client = new IlpServiceClient(
//     "127.0.0.1:6565",
//     credentials.createInsecure()
//   );
//
//   const req = new GetBalanceRequest();
//   req.setAccountid("connie");
//   client.getBalance(req, (error, response): void => {
//     if (error != null || response == null) {
//       console.log("ERRORED :(");
//       console.log(error);
//       return;
//     }
//     console.log(JSON.stringify(response));
//   });
// }

async function runILPCreateAccountTest(): Promise<void> {
  const client = new IlpServiceClient(
    "127.0.0.1:6565",
    credentials.createInsecure()
  );

  const req = new CreateAccountRequest();
  req.setAccountid("Noah");
  req.setAssetcode("XRP");
  req.setAssetscale(9);
  req.setDescription("Noah's test account");

  client.createAccount(req, (error, response): void => {
    if (error != null || response == null) {
      console.log("ERRORED :(");
      console.log(error);
      return;
    }
    console.log(JSON.stringify(response));
  });
}

//runILPGetBalanceTest();
runILPCreateAccountTest();
