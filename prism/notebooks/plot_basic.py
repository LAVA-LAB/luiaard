import os
from math import log10

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from pandas.api.types import CategoricalDtype

from util import collect_csv_files
from util import export_legend
from util import get_plotting_arguments_parser
from util import get_related_configuration
from util import make_dprint


def main(
    results_path,
    remove_legend,
    external_legend,
    show,
    output_type,
    save,
    confidence_interval,
    out_prefix,
    verbose,
    includes,
    excludes,
    plot_bounds,
    plot_errors,
    n_points=300
):
    settings = {}
    settings["prefix"] = results_path.rstrip("/")
    settings['title'] = os.path.basename(settings['prefix']).title().replace("_", " ")
    settings["out_prefix"] = out_prefix if out_prefix is not None else settings['prefix']
    os.makedirs(os.path.dirname(settings["out_prefix"]), exist_ok=True)
    dprint = make_dprint(settings['prefix'], verbose)
    opt_style = {
        "linewidth": 2,
        "color": 'black',
        "linestyle": 'dotted'
    }
    plt.style.use('./paper.mplstyle')
    csv_files = collect_csv_files(includes, excludes, settings["prefix"], verbose)
    alg_names = ["LUI", "MAP", "UCRL", "PAC"]
    alg_type = CategoricalDtype(alg_names, ordered=True)
    dtype_map = {
        'Algorithm': alg_type,
        'Accumulated Samples': 'int32',
        'Seed': 'int32',
        'Episode': 'int32',
        'Performance': 'float32',
        'Estimated Performance': 'float32',
        'Average Distance': 'float32',
        'Lower Bound': 'float32',
        'Upper Bound': 'float32',
    }

    # update settings with arbitrary yaml file
    config = get_related_configuration(csv_files[0])
    settings.update(config)
    settings['total_trajectories'] = config['iterations']
    col_list = [
        'Episode',
        'Performance',
        'Estimated Performance',
        'Average Distance',
        # 'Accumulated Samples'
    ]
    figs, axs = dict(), dict()
    figs["perf"], axs['perf'] = plt.subplots()
    if plot_errors:
        figs["model_error"], axs['model_error'] = plt.subplots()
        figs["estimation_error"], axs['estimation_error'] = plt.subplots()
    if plot_bounds:
        col_list.extend(['Lower Bound', 'Upper Bound'])
        figs["bounds"], axs['bounds'] = plt.subplots()

    episodes = [1, 100, 1000, 10001, 100001]
    episodes += list(np.unique(np.logspace(0, log10(settings['total_trajectories']), num=n_points, dtype='int')))
    episodes += [settings['total_trajectories']]
    episodes = pd.DataFrame({"Trajectory": sorted(episodes)})

    add_legend = not remove_legend
    dfs = []
    for ind, csv_file in enumerate(csv_files):
        config = get_related_configuration(csv_file)
        dprint(f"reading {csv_file}")
        new_df = pd.read_csv(
            csv_file,
            usecols=col_list,  # read subset of columns
            memory_map=True,   # load file to memory before processing it
            dtype=dtype_map,   # set dtypes to reduce memory usage
        )
        new_df.rename(columns={"Average Distance": "Model Error", "Episode": "Trajectory"}, inplace=True)
        new_df["Algorithm"] = config["algorithm"]
        new_df["Seed"] = config["seed"]
        new_df = (
            new_df
            .append(episodes, ignore_index=True)
            .drop_duplicates(subset="Trajectory", keep='first')
            .sort_values("Trajectory")
            .interpolate(method="pad")
        )
        new_df = new_df[new_df["Trajectory"].isin(episodes["Trajectory"])].reset_index(drop=True)
        if settings["spec"].startswith("Rmin"):
            new_df["Performance"] = - new_df["Performance"]
            new_df["Estimated Performance"] = - new_df["Estimated Performance"]
        dfs.append(new_df)

    df = pd.concat(dfs).reset_index(drop=True)

    df["Estimation Error"] = df["Estimated Performance"] - df["Performance"]
    df["Algorithm"] = df["Algorithm"].astype(alg_type)
    # dprint(df["Algorith"])
    dprint(df.groupby("Algorithm")["Seed"].nunique())
    dprint(df.dtypes)
    dprint("plotting Performance")
    sns.lineplot(data=df, x="Trajectory", y="Performance", hue="Algorithm",
                 ci=confidence_interval, ax=axs['perf'], legend=True)


    if plot_errors:
        dprint("plotting Average Distance")
        sns.lineplot(data=df, x="Trajectory", y="Model Error", hue="Algorithm",
                     ci=confidence_interval, ax=axs['model_error'], legend=False)
        dprint("plotting Estimation Error")
        sns.lineplot(data=df, x="Trajectory", y="Estimation Error", hue="Algorithm",
                     ci=confidence_interval, ax=axs['estimation_error'], legend=False)
    if plot_bounds:
        dprint("plotting Bounds")
        sns.lineplot(data=df, x="Trajectory", y="Lower Bound", hue="Algorithm",
                     ci=confidence_interval, ax=axs['bounds'], legend=False)
        sns.lineplot(data=df, x="Trajectory", y="Upper Bound", hue="Algorithm",
                     ci=confidence_interval, ax=axs['bounds'], legend=False)
    if external_legend:
        export_legend(axs['perf'], settings['out_prefix'], output_type, save, show)
    if add_legend:
        handles, labels = axs['perf'].get_legend_handles_labels()
        for label, ax in axs.items():
            ax.legend(handles, labels, loc='center left', bbox_to_anchor=(1, 0.5), fontsize='x-small')
    else:
        axs['perf'].get_legend().remove()
    if settings["spec"].startswith("R"):
        axs['perf'].set_ylabel("$\mathrm{R}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
    elif settings["spec"].startswith("P"):
        axs['perf'].set_ylabel("$\mathbb{P}^M_\pi(\lozenge \, \ensuremath{T})$", rotation=0, ha='right')
    else:
        raise ValueError("unexpected spec")
    for ax in axs.values():
        # ax.set_xscale('symlog')
        ax.set_xscale('log')
        # ax.set_yscale('symlog')

    if plot_bounds:
        axs['bounds'].set_ylabel("Estimates First Transition")

    axs['perf'].hlines(y=settings["trueOpt"], xmin=1, xmax=settings["iterations"], **opt_style)
    if plot_errors:
        axs['estimation_error'].hlines(y=0, xmin=1, xmax=settings["iterations"], **opt_style)

    for f in figs.values():
        f.suptitle(settings["title"])
        f.tight_layout(pad=0.2)
    if save:
        for label, fig in figs.items():
            fig.savefig(f"{settings['out_prefix']}_{label}.{output_type}", bbox_inches="tight")
    if show:
        plt.show()
    dprint("done")


if __name__ == '__main__':
    parser = get_plotting_arguments_parser()
    parser.add_argument("--plot_bounds", default=False, action="store_true")
    parser.add_argument("--plot_errors", default=False, action="store_true")
    main(**vars(parser.parse_args()))
